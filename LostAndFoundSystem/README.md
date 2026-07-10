# Campus Lost and Found Management System (Java)

A DSA project demonstrating HashMap, ArrayList, LinkedList, linear search, binary
search, and a weighted matching algorithm. Ships with three interchangeable
front ends that all share the same data structures and logic: a console app, a
desktop Swing GUI, and a full web app.

## How to compile

```
cd LostAndFoundSystem
javac -d bin src/*.java
```

## How to run

**Desktop GUI:**
```
java -cp bin Main
```
Opens a window with five tabs: Report Lost Item, Report Found Item,
Search & Match, Claim & Verify, and Records.

**Web app:**
```
java -cp bin WebServer
```
Then open **http://localhost:8080** in your browser. Same five modules,
served as a single-page web app with a JSON API backend. No external
frameworks or internet connection required - it's built entirely on the
JDK's built-in `com.sun.net.httpserver`.

Run either command from inside the `LostAndFoundSystem` folder so the
`data/` folder (where records are saved) and, for the web app, the `web/`
folder (frontend HTML) are found correctly.

## Project structure

```
LostAndFoundSystem/
  src/
    Item.java             - abstract base class for any reported item
    LostItem.java          - lost item record
    FoundItem.java          - found item record
    Claim.java                - claim/verification record
    DataStore.java              - HashMap/ArrayList/LinkedList storage + file persistence
    MatchingEngine.java          - linear search, binary search, matching/scoring algorithm
    Main.java                     - desktop GUI entry point (launches MainFrame)
    MainFrame.java                  - Swing UI: all 5 tabs/modules
    WebServer.java                    - web app entry point: JSON API + static file server
    JsonUtil.java                      - minimal hand-written JSON encoding helpers
    GuiSmokeTest.java (optional)        - throwaway QA harness used to screenshot-test the
                                           desktop UI; safe to delete, not required to run
  web/
    index.html              - single-page web frontend (HTML + CSS + JS, no external deps)
  data/                        - auto-created; stores lost_items.txt, found_items.txt, claims.txt
    images/                      - auto-created; stores uploaded item photos
  bin/                          - compiled .class files
```

## Modules (matches the 4 functional modules in the project brief)

1. **Lost Item Registration** - register a lost item, get a unique ID (e.g. L001),
   optionally attach a photo
2. **Found Item Registration** - register a found item, optionally with a photo; the
   system immediately suggests possible matching lost-item reports
3. **Search and Matching** - keyword linear search, category HashMap lookup,
   exact-date binary search, and ranked match suggestions
4. **Claim and Verification** - checks the match score plus a manual
   identifying-detail question before marking items RETURNED

The Records view lists all lost items, found items, and the claims log.
Closing the GUI (or a `POST`/registration in the web app) auto-saves data
to the `data/` folder, so records persist across restarts.

## Photos (web app only)

The web app's Report Lost and Report Found forms have an optional photo field.
The browser resizes/compresses the image client-side (max 640px, JPEG) before
sending it, so even a large phone photo stays small. The server decodes and
saves it to `data/images/<itemId>.jpg`, and serves it back at `/uploads/<file>`.
Photos show up as thumbnails in the Records table, search results, and match
suggestions - click a thumbnail to view it full size. Items registered without
a photo just show "No photo".

## Web app API (used by web/index.html, also usable directly)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/lost?mode=all\|keyword\|category\|date&q=...` | search/list lost items |
| POST | `/api/lost` | register a lost item (optional `image` field, a base64 data URL) |
| GET | `/api/found?mode=all\|keyword\|category&q=...` | search/list found items |
| POST | `/api/found` | register a found item (returns suggested matches) |
| GET | `/api/matches?lostId=L001` | ranked found-item matches for a lost item |
| POST | `/api/claim` | submit and verify a claim |
| GET | `/api/records` | full dump of lost items, found items, and claims |
| GET | `/uploads/<filename>` | serves a saved item photo |

## Publishing this project to GitHub

```
git init
git add .
git commit -m "Initial commit: Lost and Found Management System"
git branch -M main
git remote add origin <your-repo-url>
git push -u origin main
```
