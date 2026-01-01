// package model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Chess {
    // Array to store the most recent 3 board states
    private Cell[][][] boardHistory;
    private int boardHistoryIndex;
    private int boardHistoryCount;
    private boolean gameOver = false;
    // Name of winner (set when game ends)
    private String winnerName = null;
    private boolean review = false;

    // Undo limit per player
    private static final int MAX_UNDO = 3;
    public int redUndoRemaining = MAX_UNDO;
    public int blueUndoRemaining = MAX_UNDO;

    public static final int ROWS = 10;  
    public static final int COLS = 7;
    public Cell[][] board = new Cell[ROWS][COLS];

    public Chess() {
        // Initialize board history for undo
        boardHistory = new Cell[3][][];
        boardHistoryIndex = 0;
        boardHistoryCount = 0;

        redUndoRemaining = MAX_UNDO;
        blueUndoRemaining = MAX_UNDO;
    }    

    private void checkWinCondition() {
        int redCount = 0, blueCount = 0;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                Piece p = board[i][j].piece;
                if (p != null) {
                    if (p.owner.side == Side.RED) redCount++;
                    else if (p.owner.side == Side.BLUE) blueCount++;
                }
            }
        }
        if (redCount == 0) {
            String msg = bluePlayer.name + " (Blue) wins by capturing all opponent's pieces!";
            System.out.println(msg);
            try {
                logEvent(msg);
            } catch (IOException e) {
                System.err.println("Failed to log event: " + e.getMessage());
            }
            winnerName = bluePlayer.name + " (Blue)";
            gameOver = true;
        } else if (blueCount == 0) {
            String msg = redPlayer.name + " (Red) wins by capturing all opponent's pieces!";
            System.out.println(msg);
            try {
                logEvent(msg);
            } catch (IOException e) {
                System.err.println("Failed to log event: " + e.getMessage());
            }
            winnerName = redPlayer.name + " (Red)";
            gameOver = true;
        }

        // Check for den entry
        if (!gameOver) {
            if (board[1][3].piece != null && board[1][3].piece.owner.side == Side.RED) {
                String msg = redPlayer.name + " (Red) wins by entering Blue's den!";
                System.out.println(msg);
                try {
                    logEvent(msg);
                } catch (IOException e) {
                    System.err.println("Failed to log event: " + e.getMessage());
                }
                winnerName = redPlayer.name + " (Red)";
                gameOver = true;
            } else if (board[9][3].piece != null && board[9][3].piece.owner.side == Side.BLUE) {
                String msg = bluePlayer.name + " (Blue) wins by entering Red's den!";
                System.out.println(msg);
                try {
                    logEvent(msg);
                } catch (IOException e) {
                    System.err.println("Failed to log event: " + e.getMessage());
                }
                winnerName = bluePlayer.name + " (Blue)";
                gameOver = true;
            }
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public String getWinnerName() {
        return winnerName;
    }

    private void placePieces() {
        // RED pieces (shifted down)
        board[9][6].piece = new Piece(7, redPlayer); // Lion 
        board[9][0].piece = new Piece(6, redPlayer); // Tiger 
        board[8][5].piece = new Piece(3, redPlayer); // Dog
        board[8][1].piece = new Piece(2, redPlayer); // Cat
        board[7][4].piece = new Piece(5, redPlayer); // Leopard
        board[7][2].piece = new Piece(4, redPlayer); // Wolf
        board[7][0].piece = new Piece(8, redPlayer); // Elephant
        board[7][6].piece = new Piece(1, redPlayer); // Rat

        // BLUE pieces (shifted down)
        board[1][0].piece = new Piece(7, bluePlayer); // Lion
        board[1][6].piece = new Piece(6, bluePlayer); // Tiger
        board[2][1].piece = new Piece(3, bluePlayer); // Dog
        board[2][5].piece = new Piece(2, bluePlayer); // Cat
        board[3][2].piece = new Piece(5, bluePlayer); // Leopard
        board[3][4].piece = new Piece(4, bluePlayer); // Wolf
        board[3][6].piece = new Piece(8, bluePlayer); // Elephant
        board[3][0].piece = new Piece(1, bluePlayer); // Rat
    }    

    public int countRemainingPieces(Side side) {
        int count = 0;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                Piece p = board[i][j].piece;
                if (p != null && p.owner != null && p.owner.side == side) count++;
            }
        }
        return count;
    }

    // Capture logic: returns true if capture is allowed, and performs the capture
    private boolean Capture(Piece attacker, Piece defender, Cell fromCell, Cell toCell) {
        if (toCell.isTrap && toCell.denOwner == defender.owner.side) {
            toCell.piece = null;
            return true;
        }
        if (defender == null) return false;
        // Rat (1) can capture Elephant (8), but Elephant cannot capture Rat
        if (attacker.rank == 1 && defender.rank == 8) {
            if (fromCell.isRiver == toCell.isRiver) {
                toCell.piece = null;
                return true;
            } else {
                System.out.println("Rat can only capture elephant if rat is on land.");
                return false;
            }
        }
        if (attacker.rank == 8 && defender.rank == 1) {
            System.out.println("Elephant cannot capture rat.");
            return false;
        }
        if (attacker.rank == 1 || defender.rank == 1) {
            if (fromCell.isRiver != toCell.isRiver) {
                System.out.println("Rat can only capture if both pieces are in the river or both are on land.");
                return false;
            }
        }
        if (attacker.rank >= defender.rank) {
            toCell.piece = null;
            return true;
        }
        return false;
    }
    // Move a piece for the current player from (fromRow, fromCol) to (toRow, toCol)
    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        // Save a deep copy of the board before the move
        Cell[][] boardCopy = new Cell[ROWS][COLS];
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                Cell c = board[i][j];
                boardCopy[i][j] = new Cell(c.row, c.col);
                boardCopy[i][j].isRiver = c.isRiver;
                boardCopy[i][j].isTrap = c.isTrap;
                boardCopy[i][j].isDen = c.isDen;
                boardCopy[i][j].denOwner = c.denOwner;
                if (c.piece != null) {
                    boardCopy[i][j].piece = new Piece(c.piece.rank, c.piece.owner);
                }
            }
        }
        boardHistory[boardHistoryIndex] = boardCopy;
        boardHistoryIndex = (boardHistoryIndex + 1) % 6;
        if (boardHistoryCount < 6) boardHistoryCount++;
        
        Cell fromCell = board[fromRow][fromCol];
        Cell toCell = board[toRow][toCol];

        // prevent moving into the top action row
        if (toRow == 0) {
            System.out.println("Cannot move into action row.");
            return false;
        }

        int pieceRank = fromCell.piece != null ? fromCell.piece.rank : -1;
        // Tiger (6) and Lion (7) can jump across river horizontally or vertically if no rat blocks the way
        boolean isTigerOrLion = (pieceRank == 6 || pieceRank == 7);
        boolean isStraightLine = (fromRow == toRow || fromCol == toCol);
        int dRow = Math.abs(toRow - fromRow);
        int dCol = Math.abs(toCol - fromCol);
        boolean isJump = false;
        if (isTigerOrLion && isStraightLine) {
            // Check if jumping over river
            if (fromRow == toRow) {
                // Horizontal jump
                int minCol = Math.min(fromCol, toCol);
                int maxCol = Math.max(fromCol, toCol);
                if (maxCol - minCol > 1) {
                    boolean allRiver = true;
                    for (int c = minCol + 1; c < maxCol; c++) {
                        if (!board[fromRow][c].isRiver) {
                            allRiver = false;
                            break;
                        }
                        if (board[fromRow][c].piece != null && board[fromRow][c].piece.rank == 1) {
                            System.out.println("Cannot jump: rat blocks the river.");
                            return false;
                        }
                    }
                    if (allRiver && board[toRow][toCol].isRiver == false && board[fromRow][minCol+1].isRiver) {
                        isJump = true;
                    }
                }
            } else if (fromCol == toCol) {
                // Vertical jump
                int minRow = Math.min(fromRow, toRow);
                int maxRow = Math.max(fromRow, toRow);
                if (maxRow - minRow > 1) {
                    boolean allRiver = true;
                    for (int r = minRow + 1; r < maxRow; r++) {
                        if (!board[r][fromCol].isRiver) {
                            allRiver = false;
                            break;
                        }
                        if (board[r][fromCol].piece != null && board[r][fromCol].piece.rank == 1) {
                            System.out.println("Cannot jump: rat blocks the river.");
                            return false;
                        }
                    }
                    if (allRiver && board[toRow][toCol].isRiver == false && board[minRow+1][fromCol].isRiver) {
                        isJump = true;
                    }
                }
            }
        }

        if (!isJump && !((dRow == 1 && dCol == 0) || (dRow == 0 && dCol == 1))) {
            System.out.println("Pieces can only move vertically or horizontally by one square, except tiger/lion can jump river.");
            return false;
        }

        // Only rat (1) can move into or out of river
        boolean fromRiver = fromCell.isRiver;
        boolean toRiver = toCell.isRiver;
        if (!isJump && (fromRiver || toRiver) && pieceRank != 1) {
            System.out.println("Only the rat can move into or out of water.");
            return false;
        }
        if (fromCell.piece == null) {
            System.out.println("No piece at the source position.");
            return false;
        }
        if (fromCell.piece.owner != currentPlayer) {
            System.out.println("You can only move your own pieces.");
            return false;
        }
        if (toCell.piece != null && toCell.piece.owner == currentPlayer) {
            System.out.println("Cannot move to a cell occupied by your own piece.");
            return false;
        }

        // If destination has opponent's piece, try to capture
        if (toCell.piece != null && toCell.piece.owner != currentPlayer) {
            if (!Capture(fromCell.piece, toCell.piece, fromCell, toCell)) {
                System.out.println("Cannot capture: your piece's rank is too low or not allowed by special rules.");
                return false;
            }
        }

        // Move the piece
        toCell.piece = fromCell.piece;
        fromCell.piece = null;

        // Log the move (e.g. "move A7 A6")
        try {
            logEvent("move " + (char)('A' + fromCol) + fromRow + " " + (char)('A' + toCol) + toRow);
        } catch (Exception ignore) { }

        currentPlayer = (currentPlayer == redPlayer) ? bluePlayer : redPlayer;

        // Check win condition after move
        checkWinCondition();

        return true;
    }

    public boolean undo() {
        if (boardHistoryCount == 0) {
            System.out.println("No moves to undo.");
            return false;
        }

        // Check undo allowance for the player who is invoking undo
        boolean callerIsRed = (currentPlayer == redPlayer);
        if (callerIsRed && blueUndoRemaining <= 0) {
            System.out.println("No undos remaining for " + bluePlayer.name + ".");
            return false;
        }
        if (!callerIsRed && redUndoRemaining <= 0) {
            System.out.println("No undos remaining for " + redPlayer.name + ".");
            return false;
        }

        // Calculate the index of the previous board state
        int prevIndex = (boardHistoryIndex - 1 + 6) % (6);

        // Deep copy the previous board state into the current board
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                Cell src = boardHistory[prevIndex][i][j];
                board[i][j] = new Cell(src.row, src.col);
                board[i][j].isRiver = src.isRiver;
                board[i][j].isTrap = src.isTrap;
                board[i][j].isDen = src.isDen;
                board[i][j].denOwner = src.denOwner;
                if (src.piece != null) {
                    // Assign the correct owner reference
                    Player owner = (src.piece.owner.side == Side.RED) ? redPlayer : bluePlayer;
                    board[i][j].piece = new Piece(src.piece.rank, owner);
                }
            }
        }
        // Update boardHistoryIndex and boardHistoryCount
        boardHistoryIndex = prevIndex;
        boardHistoryCount--;
        // Switch current player back
        currentPlayer = (currentPlayer == redPlayer) ? bluePlayer : redPlayer;
        // decrement caller's remaining undos
        if (callerIsRed) {
            blueUndoRemaining--;
            System.out.println("Undo successful. " + bluePlayer.name + " has " + blueUndoRemaining + " undos remaining.");
        } else {
            redUndoRemaining--;
            System.out.println("Undo successful. " + redPlayer.name + " has " + redUndoRemaining + " undos remaining.");
        }
        try {
            logEvent("undo");
        } catch (IOException e) {
            System.err.println("Failed to log undo event: " + e.getMessage());
        }
        return true;
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    public static class Player {
        public String name;
        public Side side;

        public Player(Side side, String name) {
            this.side = side;
            this.name = name;
        }
    }

    public enum Side {
        RED, BLUE, NONE
    }

    private final Player redPlayer = new Player(Side.RED, "Red");
    private final Player bluePlayer = new Player(Side.BLUE, "Blue");
    private Player currentPlayer = redPlayer;

    public void setPlayerName(Side side, String name) {
        if (side == Side.RED) {
            redPlayer.name = name;
        } else if (side == Side.BLUE) {
            bluePlayer.name = name;
        }
    }
    
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setRandomPlayerNames() {
        String[] names = {"Tiger", "Dragon", "Phoenix", "Panda", "Monkey", "Crane", "Leopard", "Wolf", "Peter", "Andrew", "James", "John","Philip","Bartholomew","Matthew","Thomas","Semaj","Simon","Thaddeus","Amadeus", "Steve404"};
        List<String> nameList = new ArrayList<>(Arrays.asList(names));
        Collections.shuffle(nameList);
        redPlayer.name = nameList.get(0);
        bluePlayer.name = nameList.get(1);
    }

    public static class Piece {
        public int rank;
        public Player owner;

        public Piece(int rank, Player owner) {
            this.rank = rank;
            this.owner = owner;
        }
    }

    public static class Cell {
        public int row, col;
        public Piece piece;
        public boolean isRiver;
        public boolean isTrap;
        public boolean isDen;
        public Side denOwner;

        public Cell(int row, int col) {
            this.row = row;
            this.col = col;
            this.piece = null;
            this.isRiver = false;
            this.isTrap = false;
            this.isDen = false;
            this.denOwner = Side.NONE;
        }
    }

    // Current log file name for this game session 
    private String currentLogFileName = null;

    // Starts a new game by initializing the board and placing pieces
    public void GameSetUp(boolean rev) throws IOException {
        initializeBoardHistory();
        initializeBoard();
        // reset undo counters at new game start
        redUndoRemaining = MAX_UNDO;
        blueUndoRemaining = MAX_UNDO;
        if(rev){
            review = true;
            return;
        }
        // Create record folder if it doesn't exist
        File recordDir = new File("record");
        if (!recordDir.exists()) {
            recordDir.mkdir();
        }
        // Find next available log file name
        int logNum = getNextLogNumber(recordDir, ".record");
        String logFileName = "record/game_log_" + logNum + ".record";
        currentLogFileName = logFileName;
        try (FileWriter fw = new FileWriter(logFileName, false)) {
            fw.write("start " + redPlayer.name + " " + bluePlayer.name + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Failed to create game log: " + e.getMessage());
        }
    }

    // Helper to get next available log number in record folder
    public int getNextLogNumber(File recordDir, String type) {
        String mid;
        int diff;
        if (type.equals(".jungle")){mid = "save";diff=10;}
        else {mid = "log";diff=9;}
        String[] files = recordDir.list();
        int maxNum = 0;
        if (files != null) {
            for (String f : files) {
                if (f.matches("game_"+mid+"_\\d+\\"+type)) {
                    try{
                        int num = Integer.parseInt(f.substring(diff, f.indexOf(type)));
                        if (num > maxNum) maxNum = num;
                    } catch (NumberFormatException ignore) {}
                }
            }
        }
        return maxNum + 1;
    }

    // Logs a user event/command to the current game log file inside the record folder.
    // Falls back to top-level "game_log.record" if no current game log is set or writing fails.
    public void logEvent(String event) throws IOException {
        if (review) return; // do not log events in review mode
        String target = (currentLogFileName != null) ? currentLogFileName : "game_log.record";
        try (FileWriter fw = new FileWriter(target, true)) {
            fw.write(event + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Failed to write to log (" + target + "): " + e.getMessage());
            // If we failed writing to the target file and it wasn't the fallback, try the fallback
            if (!"game_log.record".equals(target)) {
                try (FileWriter fw = new FileWriter("game_log.record", true)) {
                    fw.write(java.time.LocalDateTime.now() + ": " + event + System.lineSeparator());
                } catch (IOException ex) {
                    System.err.println("Also failed to write to fallback game_log.record: " + ex.getMessage());
                }
            }
        }
    }

    // Initialize the board history array and counters
    private void initializeBoardHistory() {
        boardHistory = new Cell[6][][];
        boardHistoryIndex = 0;
        boardHistoryCount = 0;
    }

    private void initializeBoard() {
        for (int i = 0; i < ROWS; i++)
            for (int j = 0; j < COLS; j++)
                board[i][j] = new Cell(i, j);

        // Set rivers
        for (int i = 4; i <= 6; i++)
            for (int j = 1; j <= 2; j++)
                board[i][j].isRiver = true;
        for (int i = 4; i <= 6; i++)
            for (int j = 4; j <= 5; j++)
                board[i][j].isRiver = true;

        // Set dens (shifted down by 1 row)
        board[1][3].isDen = true;
        board[1][3].denOwner = Side.BLUE;
        board[9][3].isDen = true;
        board[9][3].denOwner = Side.RED;

        // Set traps
        int[][] redTraps = {{1,2},{1,4},{2,3}}; // shifted down
        int[][] blueTraps = {{9,2},{9,4},{8,3}}; // shifted down
        for (int[] t : redTraps) {
            board[t[0]][t[1]].isTrap = true;
            board[t[0]][t[1]].denOwner = Side.RED;

        }
        for (int[] t : blueTraps) {
            board[t[0]][t[1]].isTrap = true;
            board[t[0]][t[1]].denOwner = Side.BLUE;
        }

        placePieces();
    }

    // Show the map, remaining piece number, current and next player
    public void showMap() {
        System.out.println("\n   A  B  C  D  E  F  G");
        for (int i = 1; i < ROWS; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < COLS; j++) {
                Cell cell = board[i][j];
                if (cell.piece != null) {
                    if (cell.piece.owner.side == Side.RED) {
                        System.out.print("R" + cell.piece.rank);
                    } else {
                        System.out.print("B" + cell.piece.rank);
                    }
                } else if (cell.isRiver) {
                    System.out.print("~~");
                } else if (cell.isDen) {
                    System.out.print("DD");
                } else if (cell.isTrap) {
                    System.out.print("[]");
                } else {
                    System.out.print(" .");
                }
                System.out.print(" ");
            }
            System.out.println();
        }

        int redCount = 0, blueCount = 0;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                Piece p = board[i][j].piece;
                if (p != null) {
                    if (p.owner.side == Side.RED) redCount++;
                    else if (p.owner.side == Side.BLUE) blueCount++;
                }
            }
        }

        System.out.println("Remaining pieces: " + redPlayer.name + " (Red): " + redCount + ", " + bluePlayer.name + " (Blue): " + blueCount);
        System.out.println("Remaining undo: " + redPlayer.name + " (Red): " + redUndoRemaining + ", " + bluePlayer.name + " (Blue): " + blueUndoRemaining);
        System.out.println("Current player: " + currentPlayer.name + " (" + currentPlayer.side + ")");
        Player nextPlayer = (currentPlayer == redPlayer) ? bluePlayer : redPlayer;
        System.out.println("Next player: " + nextPlayer.name + " (" + nextPlayer.side + ")\n");
    }

    public boolean saveGameToJungle() throws IOException {
        Path dir = Paths.get("jungle");
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("Failed to create jungle directory: " + e.getMessage());
            return false;
        }

        String filename;
        int next = getNextLogNumber(dir.toFile(), ".jungle");
        filename = String.format("game_save_%d.jungle", next);

        Path file = dir.resolve(filename);
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            // Player info
            bw.write("player RED " + (redPlayer.name == null ? "" : redPlayer.name));
            bw.newLine();
            bw.write("player BLUE " + (bluePlayer.name == null ? "" : bluePlayer.name));
            bw.newLine();

            bw.write("current " + (currentPlayer == null ? "RED" : currentPlayer.side.name()));
            bw.newLine();

            bw.write("undos RED " + redUndoRemaining);
            bw.newLine();
            bw.write("undos BLUE " + blueUndoRemaining);
            bw.newLine();

            // Pieces: iterate full board and write only occupied cells
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    Cell cell = board[r][c];
                    if (cell != null && cell.piece != null) {
                        String owner = (cell.piece.owner == null) ? "RED" : cell.piece.owner.side.name();
                        bw.write(String.format("piece %d %d %s %d", r, c, owner, cell.piece.rank));
                        bw.newLine();
                    }
                }
            }

            bw.flush();
            System.out.println("Game saved to " + file.toString());
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save game file: " + e.getMessage());
            return false;
        }
    }

    public boolean loadGameFromJungle(String id) throws IOException {
        if (id == null) id = "";
        id = id.trim();
        int n = Integer.parseInt(id);
        id = String.format("%d", n);
        id = "game_save_" + id;

        Path file = Paths.get("jungle", id + ".jungle");
        if (!Files.exists(file)) {
            System.err.println("Save file not found: " + file.toString());
            return false;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to read save file: " + e.getMessage());
            return false;
        }

        // Initialize board (sets rivers/traps/den) then clear pieces
        initializeBoard();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] != null) board[r][c].piece = null;
            }
        }

        // Reset game state
        gameOver = false;
        winnerName = null;
        initializeBoardHistory();
        boardHistoryIndex = 0;
        boardHistoryCount = 0;
        currentPlayer = redPlayer; // default until parsed

        // Set defaults for undo counters in case file doesn't include them
        redUndoRemaining = MAX_UNDO;
        blueUndoRemaining = MAX_UNDO;
        review = true;
        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String lower = line.toLowerCase();
            try {
                if (lower.startsWith("player ")) {
                    // allow spaces in name: split into 3 parts
                    String[] parts = line.split("\\s+", 3);
                    if (parts.length >= 3) {
                        String side = parts[1].toUpperCase();
                        String name = parts[2];
                        if ("RED".equals(side)) redPlayer.name = name;
                        else if ("BLUE".equals(side)) bluePlayer.name = name;
                    }
                } else if (lower.startsWith("current ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        String s = parts[1].toUpperCase();
                        if ("RED".equals(s)) currentPlayer = redPlayer;
                        else if ("BLUE".equals(s)) currentPlayer = bluePlayer;
                    }
                } else if (lower.startsWith("undos ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        String side = parts[1].toUpperCase();
                        int val = Integer.parseInt(parts[2]);
                        if ("RED".equals(side)) redUndoRemaining = Math.max(0, Math.min(MAX_UNDO, val));
                        else if ("BLUE".equals(side)) blueUndoRemaining = Math.max(0, Math.min(MAX_UNDO, val));
                    } else {
                        System.err.println("Malformed undos line, skipping: " + line);
                    }
                } else if (lower.startsWith("piece ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 5) {
                        int r = Integer.parseInt(parts[1]);
                        int c = Integer.parseInt(parts[2]);
                        String ownerStr = parts[3].toUpperCase();
                        int rank = Integer.parseInt(parts[4]);
                        if (!inBounds(r, c)) {
                            System.err.println("Skipping piece out of bounds: " + line);
                            continue;
                        }
                        Player owner = "BLUE".equals(ownerStr) ? bluePlayer : redPlayer;
                        board[r][c].piece = new Piece(rank, owner);
                    } else {
                        System.err.println("Malformed piece line, skipping: " + line);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing line, skipping: '" + line + "' -> " + e.getMessage());
            }
        }

        System.out.println("Loaded save: " + file.toString());
        // Clear winner / gameOver in case save had game ended state handled differently
        gameOver = false;
        winnerName = null;
        return true;
    }
}
