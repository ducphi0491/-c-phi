C:\TunerToolsGP_Fresh>py check_db.py
FILE: C:\inetpub\wwwroot\tab\songsterr_database.db
SIZE: 531.5 MiB
TABLE COUNT: 3

TABLE: songs
COLUMNS: id (TEXT), title (TEXT), artist (TEXT), file_path (TEXT), format (TEXT)
ROWS: 1,650,639
SAMPLE 1: {'id': '', 'title': '+caro - Todo Es Por Nada', 'artist': '+caro', 'file_path': None, 'format': None}
SAMPLE 2: {'id': '8697738637882630861', 'title': '+caro - Todo Es Por Nada', 'artist': '+caro', 'file_path': '+/+caro/+caro - Todo Es Por Nada.gp4', 'format': 'gp4'}

TABLE: songs_fts
COLUMNS: id (?), title (?), artist (?), file_path (?), format (?)
ROWS: 1,650,639
SAMPLE 1: {'id': None, 'title': '+caro - Todo Es Por Nada', 'artist': '+caro', 'file_path': None, 'format': None}
SAMPLE 2: {'id': None, 'title': '+caro - Todo Es Por Nada', 'artist': '+caro', 'file_path': '+/+caro/+caro - Todo Es Por Nada.gp4', 'format': 'gp4'}

TABLE: songs_fts_content
COLUMNS: id (INTEGER), c0 (?), c1 (?), c2 (?), c3 (?), c4 (?)
ROWS: 1,650,639
SAMPLE 1: {'id': '1'}
SAMPLE 2: {'id': '2'}

READ-ONLY COMPLETE. Database was not changed.

C:\TunerToolsGP_Fresh>
