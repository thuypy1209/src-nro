import re

team2026_file = r'C:\Users\ADMIN\.gemini\antigravity\brain\d6331dab-0d70-4381-8d94-15352a8a2246\.user_uploaded\media_1793449747800_def4ff04.sql'

with open(team2026_file, 'r', encoding='utf-8', errors='ignore') as f:
    content = f.read()

inserts = re.findall(r'INSERT INTO `([^`]+)`', content)
from collections import Counter
counts = Counter(inserts)
print('Row insert blocks per table in team2026:')
for t, c in counts.items():
    print(f'  {t}: {c}')
