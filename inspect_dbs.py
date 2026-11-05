import re

team2026_file = r'C:\Users\ADMIN\.gemini\antigravity\brain\d6331dab-0d70-4381-8d94-15352a8a2246\.user_uploaded\media_1793449747800_def4ff04.sql'
nro_file = r'C:\Users\ADMIN\.gemini\antigravity\brain\d6331dab-0d70-4381-8d94-15352a8a2246\.user_uploaded\media_1793449763272_867fbe24.sql'

def get_tables(filepath):
    tables = []
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        for line in f:
            if 'CREATE TABLE' in line:
                m = re.search(r'CREATE TABLE [^`]*`([^`]+)`', line)
                if m:
                    tables.append(m.group(1))
    return tables

print('Tables in team2026:', get_tables(team2026_file))
print('Tables in nro:', get_tables(nro_file))
