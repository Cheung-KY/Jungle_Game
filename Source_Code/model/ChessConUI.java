// The UI class for console-based interaction with the Chess game
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ChessConUI {
    private Chess chess;
    private final Scanner scanner;
    private boolean loadGame= false;

    public ChessConUI() {
        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        ChessConUI ui = new ChessConUI();
        while (true) {
            System.out.println("Enter command: start | load | replay | exit");
            String cmd;
            cmd = ui.scanner.nextLine().trim().toLowerCase();
            switch (cmd) {
                case "start":
                    ui.startGame();
                    break;
                case "load":
                    ui.loadSavedGame();
                    break;
                case "replay":
                    try {
                        ui.loadFromFile();
                    } catch (IOException ioe) {
                        System.err.println("Failed to load record: " + ioe.getMessage());
                    }
                    break;
                case "exit":
                    System.out.println("Exiting...");
                    ui.scanner.close();
                    return;
                default:
                    System.out.println("Unknown command.");
            }
        }
    }

    private void startGame() {
        if(!loadGame)
        {
            chess = new Chess();
            System.out.print("Enter Red player name (leave empty for random): ");
            String redName = scanner.nextLine().trim();
            System.out.print("Enter Blue player name (leave empty for random): ");
            String blueName = scanner.nextLine().trim();

            if (redName.isEmpty() && blueName.isEmpty()) {
                chess.setRandomPlayerNames();
            } else if (redName.isEmpty()) {
                chess.setRandomPlayerNames();
                chess.setPlayerName(Chess.Side.BLUE, blueName);
            } else if (blueName.isEmpty()) {
                chess.setRandomPlayerNames();
                chess.setPlayerName(Chess.Side.RED, redName);
            } else {
                chess.setPlayerName(Chess.Side.RED, redName);
                chess.setPlayerName(Chess.Side.BLUE, blueName);
            }

            try {
                chess.GameSetUp(true);
            } catch (IOException ioe) {
                System.err.println("Failed to start game: " + ioe.getMessage());
                return;
            }
        }

        while (!chess.isGameOver()) {
            try {
                chess.showMap();
            } catch (RuntimeException e) {
                System.err.println("Failed to show map: " + e.getMessage());
            }
            System.out.print("Enter command (move [srcPos] [desPos] | undo | save | stop): ");
            String input;
            input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("undo")) {
                try {
                    chess.undo();
                } catch (RuntimeException e) {
                    System.err.println("Undo failed: " + e.getMessage());
                }
            } else if (input.equalsIgnoreCase("save")) {
                try {
                    chess.saveGameToJungle();
                } catch (IOException ioe) {
                    System.err.println("Save failed: " + ioe.getMessage());
                }
            } else if (input.equalsIgnoreCase("stop")) {
                System.out.println("Game stopped."); 
                return;
            } else if (input.toLowerCase().startsWith("move")) {
                String[] parts = input.substring(4).trim().toUpperCase().split("\\s+");
                if (parts.length != 2 || parts[0].length() != 2 || parts[1].length() != 2) {
                    System.out.println("Invalid move format. Example: move A7 A6");
                    continue;
                }
                try {
                    int fromCol = parts[0].charAt(0) - 'A';
                    int fromRow = Character.getNumericValue(parts[0].charAt(1));
                    int toCol = parts[1].charAt(0) - 'A';
                    int toRow = Character.getNumericValue(parts[1].charAt(1));
                    if (fromCol < 0 || fromCol > 6 || toCol < 0 || toCol > 6) {
                        System.out.println("Column must be A-G.");
                        continue;
                    }
                    chess.movePiece(fromRow, fromCol, toRow, toCol);
                } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                    System.out.println("Invalid move input. Example: move A7 A6");
                } catch (RuntimeException e) {
                    System.err.println("Move failed: " + e.getMessage());
                }
            } else {
                System.out.println("Unknown command.");
            }
        }
        System.out.println("Game over! Winner: " + chess.getWinnerName());
    }

    // replay a .record file and offer replay modes (auto / step)
    void loadFromFile() throws IOException {
        System.out.print("Enter record number (e.g. 1, 11), or leave empty to go back to previous page: ");
        String num;
        num = scanner.nextLine().trim();
        if (num.isEmpty()) {
            System.out.println("No record number provided.");
            return;
        }
        if (!num.matches("\\d+")) {
            System.out.println("Invalid record number.");
            return;
        }
        
        String filename = "game_log_" + num + ".record";
        java.nio.file.Path path = Paths.get("record", filename);
        System.out.println("Attempting to load: " + path.toString());

        List<String> lines;
        try {
            if (!Files.exists(path)) {
                System.out.println("Record file not found: " + path.toString());
                return;
            }
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            System.out.println("Failed to read file: " + e.getMessage());
            return;
        }

        List<String> cmds = new ArrayList<>();
        for (String ln : lines) {
            if (ln == null) continue;
            String t = ln.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("#")) continue;
            cmds.add(t);
        }

        if (cmds.isEmpty()) {
            System.out.println("No commands found in file.");
            return;
        }

        System.out.println("Loaded " + cmds.size() + " commands.");
        String mode;
        while (true) {
            System.out.print("Replay mode? (auto | step | cancel): ");
            mode = scanner.nextLine().trim().toLowerCase();
            if (mode.equals("auto") || mode.equals("step") || mode.equals("cancel")) break;
            System.out.println("Enter 'auto', 'step', or 'cancel'.");
        }
        if (mode.equals("cancel")) {
            System.out.println("Load cancelled.");
            return;
        }

        // prepare a fresh game for replay
        chess = new Chess();
        boolean stopReplay = false;
        boolean gameStarted = false; // track whether GameSetUp() has been called

        for (String cmdLine : cmds) {
            System.out.println(">> " + cmdLine);
            if (cmdLine == null || cmdLine.trim().isEmpty()) continue;
            try {
                String lower = cmdLine.toLowerCase();
                if (lower.startsWith("start")) {
                    String[] tok = cmdLine.split("\\s+");
                    if (tok.length >= 3) {
                        chess.setPlayerName(Chess.Side.RED, tok[1]);
                        chess.setPlayerName(Chess.Side.BLUE, tok[2]);
                    } else if (tok.length == 2) {
                        chess.setRandomPlayerNames();
                        chess.setPlayerName(Chess.Side.RED, tok[1]);
                    } else {
                        chess.setRandomPlayerNames();
                    }
                    chess.GameSetUp(true);
                    gameStarted = true;
                } else if (lower.startsWith("move ")) {
                    // ensure the game is initialized before applying moves
                    if (!gameStarted) {
                        System.out.println("No explicit start found in record - starting game with random names.");
                        chess.setRandomPlayerNames();
                        chess.GameSetUp(true);
                        gameStarted = true;
                    }

                    String payload = cmdLine.substring(5).trim().toUpperCase();
                    String[] parts = payload.split("\\s+"); 
                    if (parts.length == 2 && parts[0].length() == 2 && parts[1].length() == 2) {
                        try {
                            int fromCol = parts[0].charAt(0) - 'A';
                            int fromRow = Character.getNumericValue(parts[0].charAt(1));
                            int toCol = parts[1].charAt(0) - 'A';
                            int toRow = Character.getNumericValue(parts[1].charAt(1));
                            System.out.println(parts[0] + " -> " + parts[1]);
                            chess.movePiece(fromRow, fromCol, toRow, toCol);
                        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                            System.out.println("Skipping malformed move: " + cmdLine);
                        }
                    } else {
                        System.out.println("Skipping malformed move: " + cmdLine);
                    }
                } else if (lower.equals("undo")) {
                    if (!gameStarted) {
                        System.out.println("Ignoring undo before game start.");
                    } else {
                        chess.undo();
                    }
                } else if (lower.equals("stop")) {
                    System.out.println("Stop command encountered in record.");
                    stopReplay = true;
                } else {
                    System.out.println("Unknown command in record, ignoring: " + cmdLine);
                }
            } catch (RuntimeException e) {
                System.out.println("Error executing command '" + cmdLine + "': " + e.getMessage());
            }

            try {
                chess.showMap();
            } catch (RuntimeException ignore) { }

            if (chess.isGameOver()) {
                System.out.println("Game finished during replay. Winner: " + chess.getWinnerName());
                break;
            }
            if (stopReplay) break;

            if (mode.equals("step")) {
                System.out.print("Press Enter to continue (or 'cancel' to stop replay): ");
                try {
                    String stepInput = scanner.nextLine().trim().toLowerCase();
                    if (stepInput.equals("cancel")) {
                        System.out.println("Replay cancelled by user.");
                        return;
                    }
                } catch (NoSuchElementException | IllegalStateException e) {  
                    System.err.println("Input error while stepping: " + e.getMessage());
                    return;
                }
            } else {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        if (!chess.isGameOver() && !stopReplay) {
            System.out.println("Replay finished. Game not ended or no winner.");
            if (chess.isGameOver()) System.out.println("Winner: " + chess.getWinnerName());
        }
    }

    // load saved .jungle game and allow continuing play
    void loadSavedGame() {
        System.out.print("Enter save id (e.g. 1), or leave empty to go back to previous page: "); 
        String id;
        id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            System.out.println("No save id provided.");
            return;
        }
        else if(!id.matches("\\d+")){
            System.out.println("Please provide integer only.");
            return;
        }

        // Actually load into our chess instance and continue playing
        chess = new Chess();
        try {
            if (!chess.loadGameFromJungle(id)) {
                System.out.println("Failed to load save.");
                return;
            }
            else{
                loadGame = true;
                startGame();
            }
        } catch (IOException ioe) {
            System.out.println("Failed to load save into chess instance: " + ioe.getMessage());
            return;
        }
        System.out.println("Save loaded. You can now continue the game.");
    }
}