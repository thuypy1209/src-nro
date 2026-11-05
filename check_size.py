import re

team2026_file = r'C:\Users\ADMIN\.gemini\antigravity\brain\d6331dab-0d70-4381-8d94-15352a8a2246\.user_uploaded\media_1793449747800_def4ff04.sql'

with open(team2026_file, 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

# Let's import team2026 into a temporary database `team2026_temp` in MySQL to inspect and convert it cleanly with SQL/Python!
print('File size:', len(text))
