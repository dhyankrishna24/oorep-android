#!/usr/bin/env python3
"""
Convert OOREP PostgreSQL dump into an optimized SQLite database for the OOREP Android app.
Creates indexes for high-performance offline symptom search, rubric navigation,
repertorization, and Materia Medica consultation.
"""

import sys
import os
import gzip
import sqlite3
import argparse
import time

def parse_val(val):
    if val == r'\N' or val == '':
        return None
    return val

def build_db(dump_path, output_path):
    print(f"Starting conversion from {dump_path} to {output_path}...")
    start_time = time.time()

    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    if os.path.exists(output_path):
        os.remove(output_path)

    conn = sqlite3.connect(output_path)
    cur = conn.cursor()

    # Performance pragmas for bulk load
    cur.execute("PRAGMA page_size = 4096;")
    cur.execute("PRAGMA synchronous = OFF;")
    cur.execute("PRAGMA journal_mode = MEMORY;")
    cur.execute("PRAGMA cache_size = 100000;")

    # Schema creation
    print("Creating tables...")
    cur.executescript("""
    CREATE TABLE info (
        abbrev TEXT PRIMARY KEY,
        title TEXT,
        language TEXT,
        authorlastname TEXT,
        authorfirstname TEXT,
        yearr TEXT,
        publisher TEXT,
        license TEXT,
        edition TEXT,
        displaytitle TEXT
    );

    CREATE TABLE chapter (
        abbrev TEXT,
        id INTEGER,
        name TEXT,
        PRIMARY KEY (abbrev, id)
    );

    CREATE TABLE remedy (
        id INTEGER PRIMARY KEY,
        nameabbrev TEXT NOT NULL,
        namelong TEXT,
        namealt TEXT
    );

    CREATE TABLE rubric (
        abbrev TEXT NOT NULL,
        id INTEGER NOT NULL,
        mother INTEGER,
        ismother INTEGER DEFAULT 0,
        chapterid INTEGER,
        fullpath TEXT,
        path TEXT,
        textt TEXT,
        PRIMARY KEY (abbrev, id)
    );

    CREATE TABLE rubricremedy (
        abbrev TEXT NOT NULL,
        rubricid INTEGER NOT NULL,
        remedyid INTEGER NOT NULL,
        weight INTEGER NOT NULL,
        chapterid INTEGER,
        PRIMARY KEY (abbrev, rubricid, remedyid)
    );

    CREATE TABLE mminfo (
        id INTEGER PRIMARY KEY,
        abbrev TEXT,
        lang TEXT,
        fulltitle TEXT,
        authorlastname TEXT,
        authorfirstname TEXT,
        publisher TEXT,
        yearr TEXT,
        displaytitle TEXT
    );

    CREATE TABLE mmchapter (
        id INTEGER PRIMARY KEY,
        mminfo_id INTEGER,
        heading TEXT,
        remedy_id INTEGER
    );

    CREATE TABLE mmsection (
        id INTEGER PRIMARY KEY,
        mmchapter_id INTEGER,
        depth INTEGER,
        parent_sec_id INTEGER,
        succ_sec_id INTEGER,
        heading TEXT,
        content TEXT
    );

    -- Local case storage for clinical repertorization
    CREATE TABLE caze (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        header TEXT NOT NULL,
        date_ TEXT NOT NULL,
        description TEXT
    );

    CREATE TABLE cazerubric (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        cazeid INTEGER NOT NULL,
        abbrev TEXT NOT NULL,
        rubricid INTEGER NOT NULL,
        weight INTEGER DEFAULT 1,
        is_eliminating INTEGER DEFAULT 0,
        label TEXT,
        FOREIGN KEY (cazeid) REFERENCES caze(id) ON DELETE CASCADE
    );
    """)

    current_table = None
    batch = []
    batch_size = 50000

    table_queries = {
        'info': "INSERT INTO info VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        'chapter': "INSERT INTO chapter VALUES (?, ?, ?)",
        'remedy': "INSERT INTO remedy VALUES (?, ?, ?, ?)",
        'rubric': "INSERT INTO rubric VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        'rubricremedy': "INSERT OR IGNORE INTO rubricremedy VALUES (?, ?, ?, ?, ?)",
        'mminfo': "INSERT INTO mminfo VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        'mmchapter': "INSERT INTO mmchapter VALUES (?, ?, ?, ?)",
        'mmsection': "INSERT INTO mmsection VALUES (?, ?, ?, ?, ?, ?, ?)"
    }

    counts = {t: 0 for t in table_queries}

    def flush_batch(tbl, rows):
        if not rows:
            return
        cur.executemany(table_queries[tbl], rows)
        counts[tbl] += len(rows)

    open_fn = gzip.open if dump_path.endswith('.gz') else open
    with open_fn(dump_path, 'rt', encoding='utf-8', errors='ignore') as f:
        for line in f:
            if line.startswith('COPY public.'):
                if current_table and batch:
                    flush_batch(current_table, batch)
                    batch = []
                    conn.commit()

                tbl_name = line.split()[1].replace('public.', '')
                if tbl_name in table_queries:
                    current_table = tbl_name
                    print(f"Reading {tbl_name}...")
                else:
                    current_table = None
                continue

            if current_table:
                if line.startswith(r'\.'):
                    flush_batch(current_table, batch)
                    batch = []
                    conn.commit()
                    print(f"Finished {current_table}: {counts[current_table]} rows")
                    current_table = None
                    continue

                parts = line.rstrip('\r\n').split('\t')

                if current_table == 'info':
                    # info (abbrev, title, languag, authorlastname, authorfirstname, yearr, publisher, license, edition, access, displaytitle)
                    # We map to: abbrev, title, language, authorlastname, authorfirstname, yearr, publisher, license, edition, displaytitle
                    row = (
                        parse_val(parts[0]), parse_val(parts[1]), parse_val(parts[2]),
                        parse_val(parts[3]), parse_val(parts[4]), parse_val(parts[5]),
                        parse_val(parts[6]), parse_val(parts[7]), parse_val(parts[8]),
                        parse_val(parts[10]) if len(parts) > 10 else None
                    )
                    batch.append(row)

                elif current_table == 'chapter':
                    # chapter (abbrev, id, textt)
                    row = (parse_val(parts[0]), int(parts[1]), parse_val(parts[2]))
                    batch.append(row)

                elif current_table == 'remedy':
                    # remedy (id, nameabbrev, namelong, namealt)
                    row = (int(parts[0]), parse_val(parts[1]), parse_val(parts[2]), parse_val(parts[3]))
                    batch.append(row)

                elif current_table == 'rubric':
                    # rubric (abbrev, id, mother, ismother, chapterid, fullpath, path, textt)
                    mother = int(parts[2]) if parse_val(parts[2]) else None
                    ismother = 1 if parts[3] == 't' else 0
                    chapterid = int(parts[4]) if parse_val(parts[4]) else None
                    row = (
                        parse_val(parts[0]), int(parts[1]), mother, ismother,
                        chapterid, parse_val(parts[5]), parse_val(parts[6]),
                        parse_val(parts[7]) if len(parts) > 7 else None
                    )
                    batch.append(row)

                elif current_table == 'rubricremedy':
                    # rubricremedy (abbrev, rubricid, remedyid, weight, chapterid)
                    row = (
                        parse_val(parts[0]), int(parts[1]), int(parts[2]),
                        int(parts[3]), int(parts[4]) if len(parts) > 4 and parse_val(parts[4]) else None
                    )
                    batch.append(row)

                elif current_table == 'mminfo':
                    # mminfo (id, abbrev, lang, fulltitle, authorlastname, authorfirstname, publisher, yearr, license, access, displaytitle)
                    row = (
                        int(parts[0]), parse_val(parts[1]), parse_val(parts[2]),
                        parse_val(parts[3]), parse_val(parts[4]), parse_val(parts[5]),
                        parse_val(parts[6]), parse_val(parts[7]),
                        parse_val(parts[10]) if len(parts) > 10 else None
                    )
                    batch.append(row)

                elif current_table == 'mmchapter':
                    # mmchapter (id, mminfo_id, heading, remedy_id)
                    remedy_id = int(parts[3]) if len(parts) > 3 and parse_val(parts[3]) else None
                    row = (int(parts[0]), int(parts[1]), parse_val(parts[2]), remedy_id)
                    batch.append(row)

                elif current_table == 'mmsection':
                    # mmsection (id, mmchapter_id, depth, parent_sec_id, succ_sec_id, heading, content)
                    parent_id = int(parts[3]) if parse_val(parts[3]) else None
                    succ_id = int(parts[4]) if parse_val(parts[4]) else None
                    row = (
                        int(parts[0]), int(parts[1]), int(parts[2]),
                        parent_id, succ_id, parse_val(parts[5]),
                        parse_val(parts[6]) if len(parts) > 6 else None
                    )
                    batch.append(row)

                if len(batch) >= batch_size:
                    flush_batch(current_table, batch)
                    batch = []

    if current_table and batch:
        flush_batch(current_table, batch)
        batch = []
        conn.commit()

    print("Building indexes...")
    cur.executescript("""
    CREATE INDEX IF NOT EXISTS idx_rubric_fullpath ON rubric(fullpath);
    CREATE INDEX IF NOT EXISTS idx_rubric_abbrev_ch ON rubric(abbrev, chapterid);
    CREATE INDEX IF NOT EXISTS idx_rubric_mother ON rubric(mother);
    CREATE INDEX IF NOT EXISTS idx_rr_rubric ON rubricremedy(abbrev, rubricid);
    CREATE INDEX IF NOT EXISTS idx_rr_remedy ON rubricremedy(remedyid);
    CREATE INDEX IF NOT EXISTS idx_remedy_name ON remedy(nameabbrev);
    CREATE INDEX IF NOT EXISTS idx_remedy_long ON remedy(namelong);
    CREATE INDEX IF NOT EXISTS idx_mmchapter_remedy ON mmchapter(remedy_id);
    CREATE INDEX IF NOT EXISTS idx_mmsection_chap ON mmsection(mmchapter_id);
    """)

    conn.commit()

    print("Optimizing database...")
    cur.execute("PRAGMA optimize;")
    cur.execute("VACUUM;")
    conn.commit()
    conn.close()

    elapsed = time.time() - start_time
    file_size_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"Database built successfully in {elapsed:.1f}s! Size: {file_size_mb:.2f} MB")

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Convert OOREP PostgreSQL dump to SQLite")
    parser.add_argument("--dump", required=True, help="Path to oorep.sql.gz")
    parser.add_argument("--output", required=True, help="Path to output sqlite db")
    args = parser.parse_args()

    build_db(args.dump, args.output)
