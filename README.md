# Campus Lost and Found Management System (Java, Swing GUI)

A desktop DSA project demonstrating HashMap, ArrayList, LinkedList, linear search,
binary search, and a weighted matching algorithm, with a Swing graphical interface.

## How to compile

```
cd LostAndFoundSystem
javac -d bin src/*.java
```

## How to run

```
java -cp bin Main
```

Run this from inside the `LostAndFoundSystem` folder so the `data/` folder
(where records are saved) is created alongside it. A window will open with
five tabs: Report Lost Item, Report Found Item, Search & Match, Claim & Verify,
and Records.

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
    Main.java                     - GUI entry point (launches MainFrame)
    MainFrame.java                  - Swing UI: all 5 tabs/modules
    GuiSmokeTest.java (optional)      - throwaway QA harness used to screenshot-test the UI;
                                         safe to delete, not required to run the app
  data/                        - auto-created; stores lost_items.txt, found_items.txt, claims.txt
  bin/                          - compiled .class files
```

## Modules (matches the 4 functional modules in the project brief)

1. **Lost Item Registration** - "Report Lost Item" tab
2. **Found Item Registration** - "Report Found Item" tab (also auto-suggests possible
   lost-item matches right after you submit)
3. **Search and Matching** - "Search & Match" tab: keyword linear search, category
   HashMap lookup, exact-date binary search, and ranked match suggestions
4. **Claim and Verification** - "Claim & Verify" tab: checks the match score plus a
   manual identifying-detail question before marking items RETURNED

The "Records" tab lists all lost items, found items, and the claims log, with a
Refresh button. Closing the window auto-saves all data to the `data/` folder.

## Publishing this project to GitHub

See the commands your assistant gave you in chat, or run from this folder:

```
git init
git add .
git commit -m "Initial commit: Lost and Found Management System"
git branch -M main
git remote add origin <your-repo-url>
git push -u origin main
```
