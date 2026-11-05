import re

team2026_file = r'C:\Users\ADMIN\.gemini\antigravity\brain\d6331dab-0d70-4381-8d94-15352a8a2246\.user_uploaded\media_1793449747800_def4ff04.sql'

with open(team2026_file, 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

# Accounts
acc_insert = re.search(r'INSERT INTO `account`[^(]+VALUES\s*(.+?);', text, re.DOTALL)
if acc_insert:
    val_str = acc_insert.group(1).strip()
    rows = re.findall(r'\(([^;]+?)\)[,;]', val_str + ';')
    print(f'Total accounts in team2026: {len(rows)}')
    for r in rows:
        parts = [p.strip().strip("'") for p in r.split(',')]
        if len(parts) >= 3:
            print(f'  Account ID: {parts[0]}, User: {parts[1]}, Pass: {parts[2]}')

# Players
pl_insert = re.search(r'INSERT INTO `player`[^(]+VALUES\s*(.+?);', text, re.DOTALL)
if pl_insert:
    val_str = pl_insert.group(1).strip()
    # Find player ids and names
    print('\nPlayers in team2026:')
    p_rows = re.findall(r'\((\d+),\s*(\d+),\s*\'([^\']+)\'', text)
    for p in p_rows:
        print(f'  Player ID: {p[0]}, Account ID: {p[1]}, Name: {p[2]}')

# Clans
clan_insert = re.search(r'INSERT INTO `clan`[^(]+VALUES\s*(.+?);', text, re.DOTALL)
if clan_insert:
    print('\nClan data found in team2026!')
