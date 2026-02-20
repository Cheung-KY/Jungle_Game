import java.awt.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.*;
// import java.util.*;

public class ChessUI extends JFrame {
    private Chess chess;
    private final JButton[][] boardButtons;
    private JLabel statusLabel;
    private JLabel commentLabel;
    private int selectedRow = -1, selectedCol = -1;
    private JPanel boardPanel;
    private JPanel controlPanel;
    private JPanel startPanel;

    public ChessUI() {
    chess = new Chess();
        setTitle("Jungle Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

    boardPanel = new JPanel(new GridLayout(Chess.ROWS, Chess.COLS));
        boardButtons = new JButton[Chess.ROWS][Chess.COLS];
        for (int i = 0; i < Chess.ROWS; i++) {
            for (int j = 0; j < Chess.COLS; j++) {
                JButton btn = new JButton();
                btn.setFont(new Font("Arial", Font.BOLD, 16));
                final int row = i, col = j;
                btn.addActionListener(e -> handleCellClick(row, col));
                boardButtons[i][j] = btn;
                boardPanel.add(btn);
            }
        }
    add(boardPanel, BorderLayout.CENTER);

        // Legend panel to explain rank meanings
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new GridLayout(8, 1));
        for (int r = 1; r <= 8; r++) {
            String name = rankName(r);
            JLabel lbl = new JLabel(r + " - " + name);
            lbl.setHorizontalAlignment(SwingConstants.LEFT);
            legendPanel.add(lbl);
        }
        add(legendPanel, BorderLayout.EAST);

    controlPanel = new JPanel();
        
        // Comment / feedback label at the bottom
        commentLabel = new JLabel("Welcome to Jungle Chess. Press Start to begin.");
        commentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(commentLabel, BorderLayout.PAGE_END);

        statusLabel = new JLabel();
        add(statusLabel, BorderLayout.NORTH);

        setSize(700, 800);
        setLocationRelativeTo(null);
        // Initially hide the board and legend until user starts
        boardPanel.setVisible(false);
        controlPanel.setVisible(false);
        // create and show start menu
        buildStartMenu();
        setVisible(true);
    }

    // Build a simple start menu allowing player name entry or random names
    private void buildStartMenu() {
        startPanel = new JPanel();
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Jungle Chess");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setAlignmentX(CENTER_ALIGNMENT);
        startPanel.add(Box.createVerticalStrut(20));
        startPanel.add(title);
        startPanel.add(Box.createVerticalStrut(20));

        JPanel namesPanel = new JPanel(new GridLayout(2,2,5,5));
        namesPanel.add(new JLabel("Red player name:"));
        JTextField redNameField = new JTextField();
        namesPanel.add(redNameField);
        namesPanel.add(new JLabel("Blue player name:"));
        JTextField blueNameField = new JTextField();
        namesPanel.add(blueNameField);
        namesPanel.setMaximumSize(new Dimension(400,80));
        startPanel.add(namesPanel);
        startPanel.add(Box.createVerticalStrut(10));

        JPanel btnRow = new JPanel();
        JButton startBtn = new JButton("Start");
        JButton randomBtn = new JButton("Random Names");
        JButton loadBtn = new JButton("Load Game");
        JButton replayBtn = new JButton("Replay Record");
        btnRow.add(startBtn);
        btnRow.add(randomBtn);
        btnRow.add(loadBtn);
        btnRow.add(replayBtn);
        startPanel.add(btnRow);

        // action: load a saved .jungle file and continue
        loadBtn.addActionListener(e -> {
            String id = JOptionPane.showInputDialog(this, "Enter save id (e.g. 01 or 21):");
            if (id == null) return; // cancelled
            id = id.trim();
            if (id.matches("\\d")) id = "0" + id;
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No save id provided.", "Load", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Chess loaded = new Chess();
            boolean ok = false;
            try {
                ok = loaded.loadGameFromJungle(id);
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(this, "Failed to load save: " + ioe.getMessage(), "Load", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Failed to load save: " + id, "Load", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // replace current game with loaded one and show board
            chess = loaded;
            if (startPanel != null) {
                remove(startPanel);
                startPanel = null;
            }
            boardPanel.setVisible(true);
            controlPanel.setVisible(true);
            updateBoard();
            updateStatus();
            commentLabel.setText("Loaded save " + id + ". " + chess.getPlayerName(chess.getCurrentPlayer().side) + " to move.");
            revalidate();
            repaint();
        });

        // action: replay a record file (auto or step)
        replayBtn.addActionListener(e -> {
            String id = JOptionPane.showInputDialog(this, "Enter record number (e.g. 01 or 21):");
            if (id == null) return;
            id = id.trim();
            if (id.matches("\\d")) id = "0" + id;
            if (!id.matches("\\d{2}")) {
                JOptionPane.showMessageDialog(this, "Invalid id. Use two digits like 01 or 21.", "Replay", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String[] options = {"auto", "step", "cancel"};
            String mode = (String) JOptionPane.showInputDialog(this, "Replay mode:", "Replay Mode",
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (mode == null || mode.equals("cancel")) return;
            // run replay in background
            try {
                replayRecord(id, mode.equals("step"));
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(this, "Failed to load replay record: " + ioe.getMessage(), "Replay", JOptionPane.ERROR_MESSAGE);
            }
        });

        // action: randomize names and start
        randomBtn.addActionListener(e -> {
            chess.setRandomPlayerNames();
            startGameSetup();
        });

        // action: start with provided names (randomize any empty name)
        startBtn.addActionListener(e -> {
            String r = redNameField.getText().trim();
            String b = blueNameField.getText().trim();
            if (r.isEmpty() && b.isEmpty()) {
                chess.setRandomPlayerNames();
            } else if (r.isEmpty()) {
                // randomize red by picking from the random list but avoid duplicate
                chess.setRandomPlayerNames();
                chess.setPlayerName(Chess.Side.BLUE, b);
            } else if (b.isEmpty()) {
                chess.setRandomPlayerNames();
                chess.setPlayerName(Chess.Side.RED, r);
            } else {
                chess.setPlayerName(Chess.Side.RED, r);
                chess.setPlayerName(Chess.Side.BLUE, b);
            }
            startGameSetup();
        });

        add(startPanel, BorderLayout.WEST);
        revalidate();
        repaint();
    }

    // Common start setup called by start menu actions
    private void startGameSetup() {
        // remove start panel and show board
        if (startPanel != null) {
            remove(startPanel);
            startPanel = null;
        }
        try {
            chess.GameSetUp(false);
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(this, "Failed to start game: " + ioe.getMessage(), "Start", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Make the already-added panels visible and refresh the layout
        boardPanel.setVisible(true);
        controlPanel.setVisible(true);
        // Update UI and force layout refresh
        updateBoard();
        updateStatus();
        // Re-pack to ensure components are shown
        pack();
        commentLabel.setText("Game started. " + chess.getPlayerName(chess.getCurrentPlayer().side) + " to move.");
        revalidate();
        repaint();
    }

    private void handleCellClick(int row, int col) {
        // If the cell has an action label, perform that action
        Chess.Cell clicked = chess.board[row][col];
        if (clicked.actionLabel != null) {
            String act = clicked.actionLabel;
            if ("Undo".equalsIgnoreCase(act)) {
                chess.undo();
                updateBoard();
                updateStatus();
                commentLabel.setText("Undo performed.");
                return;
            } else if ("Stop".equalsIgnoreCase(act)) {
                // return to start menu
                boardPanel.setVisible(false);
                controlPanel.setVisible(false);
                selectedRow = selectedCol = -1;
                chess = new Chess();
                buildStartMenu();
                commentLabel.setText("Returned to start menu.");
                return;
            } else if ("Save".equalsIgnoreCase(act)) {
                try {
                    chess.saveGameToJungle();
                    commentLabel.setText("Game saved.");
                } catch (IOException ioe) {
                    commentLabel.setText("Failed to save game: " + ioe.getMessage());
                }
                return;
            }
        }

        if (selectedRow == -1 && selectedCol == -1) {
            if (chess.board[row][col].piece != null && chess.board[row][col].piece.owner == chess.getCurrentPlayer()) {
                selectedRow = row;
                selectedCol = col;
                boardButtons[row][col].setBackground(Color.YELLOW);
                commentLabel.setText("Selected piece at (" + row + "," + col + "). Click destination to move.");
            }
        } else {
            boolean moved = chess.movePiece(selectedRow, selectedCol, row, col);
            if (moved) {
                updateBoard();
                updateStatus();
                commentLabel.setText("Moved from (" + selectedRow + "," + selectedCol + ") to (" + row + "," + col + ").");
                // Check for game over and show win dialog if needed
                if (chess.isGameOver()) {
                    SwingUtilities.invokeLater(() -> showWinDialog());
                }
            } else {
                commentLabel.setText("Invalid move from (" + selectedRow + "," + selectedCol + ") to (" + row + "," + col + ").");
            }
            boardButtons[selectedRow][selectedCol].setBackground(null);
            selectedRow = selectedCol = -1;
        }
    }

    private void setInteractiveEnabled(boolean enabled) {
        // disable/enable board buttons
        for (int i = 0; i < Chess.ROWS; i++) {
            for (int j = 0; j < Chess.COLS; j++) {
                boardButtons[i][j].setEnabled(enabled);
            }
        }
        // disable/enable control panel buttons (if any)
        if (controlPanel != null) {
            for (java.awt.Component c : controlPanel.getComponents()) {
                c.setEnabled(enabled);
            }
        }
        // also disable/enable window closing to avoid mid-replay interference (optional)
        // setDefaultCloseOperation(enabled ? JFrame.EXIT_ON_CLOSE : JFrame.DO_NOTHING_ON_CLOSE);
    }

    private void updateBoard() {
        for (int i = 0; i < Chess.ROWS; i++) {
            for (int j = 0; j < Chess.COLS; j++) {
                Chess.Cell cell = chess.board[i][j];
                JButton btn = boardButtons[i][j];
                if (cell.piece != null) {
                    String prefix = cell.piece.owner.side == Chess.Side.RED ? "R" : "B";
                    String name = rankName(cell.piece.rank);
                    // Use HTML to show rank and name on two lines
                    btn.setText(String.format("<html>%s%d<br/>%s</html>", prefix, cell.piece.rank, name));
                } else if (cell.isRiver) {
                    btn.setText("~~");
                } else if (cell.actionLabel != null) {
                    // show action label (e.g., Undo/Stop/Exit)
                    btn.setText(cell.actionLabel);
                } else if (cell.isDen) {
                    btn.setText("Den");
                } else if (cell.isTrap) {
                    btn.setText("Trap");
                } else {
                    // For the action row (row 0) show placeholders for empty cells
                    if (i == 0) btn.setText("===");
                    else btn.setText("");
                }
                btn.setBackground(null);
            }
        }
    }

    // Helper to map rank number to descriptive name
    private String rankName(int rank) {
        switch (rank) {
            case 1: return "Rat";
            case 2: return "Cat";
            case 3: return "Dog";
            case 4: return "Wolf";
            case 5: return "Leopard";
            case 6: return "Tiger";
            case 7: return "Lion";
            case 8: return "Elephant";
            default: return "Unknown";
        }
    }

    private void updateStatus() {
        Chess.Player current = chess.getCurrentPlayer();
        String sideLetter = current.side == Chess.Side.RED ? "R" : "B";
        // Count remaining pieces
        int redCount = 0, blueCount = 0;
        for (int i = 0; i < Chess.ROWS; i++) {
            for (int j = 0; j < Chess.COLS; j++) {
                Chess.Piece p = chess.board[i][j].piece;
                if (p != null) {
                    if (p.owner.side == Chess.Side.RED) redCount++;
                    else if (p.owner.side == Chess.Side.BLUE) blueCount++;
                }
            }
        }
        String status = String.format("Current player: %s (%s) — Remaining: Red=%d Blue=%d — Undo left: Red=%d Blue=%d", chess.getPlayerName(current.side), sideLetter, redCount, blueCount, chess.redUndoRemaining, chess.blueUndoRemaining);
        statusLabel.setText(status);
    }

    // Show a modal dialog with winner information when the game is over
    private void showWinDialog() {
        String winner = chess.getWinnerName();
        int redLeft = chess.countRemainingPieces(Chess.Side.RED);
        int blueLeft = chess.countRemainingPieces(Chess.Side.BLUE);

        JDialog dialog = new JDialog(this, "Game Over", true);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        dialog.add(Box.createVerticalStrut(10));
        JLabel title = new JLabel(winner + " wins!");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setAlignmentX(CENTER_ALIGNMENT);
        dialog.add(title);
        dialog.add(Box.createVerticalStrut(10));
        JLabel info = new JLabel(String.format("Remaining - Red: %d, Blue: %d", redLeft, blueLeft));
        info.setAlignmentX(CENTER_ALIGNMENT);
        dialog.add(info);
        dialog.add(Box.createVerticalStrut(15));

        JPanel btns = new JPanel();
        JButton toStart = new JButton("Back to Start");
        JButton exit = new JButton("Exit");
        btns.add(toStart);
        btns.add(exit);
        dialog.add(btns);

        toStart.addActionListener(e -> {
            dialog.dispose();
            // Reset UI to start menu
            boardPanel.setVisible(false);
            controlPanel.setVisible(false);
            selectedRow = selectedCol = -1;
            chess = new Chess();
            buildStartMenu();
            statusLabel.setText("");
            commentLabel.setText("Returned to start menu.");
        });
        exit.addActionListener(e -> System.exit(0));

       dialog.pack();
       dialog.setLocationRelativeTo(this);
       dialog.setVisible(true);
    }
    // Play a record file (record/game_log_XX.record). stepMode == true waits for user between commands.
    private void replayRecord(String id, boolean stepMode) throws IOException {
        Path path = Paths.get("record", "game_log_" + id + ".record");
        if (!Files.exists(path)) {
            JOptionPane.showMessageDialog(this, "Record file not found: " + path.toString(), "Replay", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // remove start panel and show board
        if (startPanel != null) {
            remove(startPanel);
            startPanel = null;
        }
        // Make the already-added panels visible and refresh the layout
        boardPanel.setVisible(true);
        controlPanel.setVisible(true);

         // disable interactive UI so user cannot click during replay
        setInteractiveEnabled(false);

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                java.util.List<String> lines;
                try {
                    lines = Files.readAllLines(path);
                } catch (IOException ex) {
                    publish("ERROR: " + ex.getMessage());
                    return null;
                }

                // prepare fresh game for replay
                chess = new Chess();
                boolean started = false;

                for (String raw : lines) {
                    String cmd = raw == null ? "" : raw.trim();
                    if (cmd.isEmpty() || cmd.startsWith("#")) continue;
                    publish(">> " + cmd);

                    try {
                        String lower = cmd.toLowerCase();
                        if (lower.startsWith("start")) {
                            String[] tok = cmd.split("\\s+");
                            if (tok.length >= 3) {
                                chess.setPlayerName(Chess.Side.RED, tok[1]);
                                chess.setPlayerName(Chess.Side.BLUE, tok[2]);
                            } else if (tok.length == 2) {
                                chess.setPlayerName(Chess.Side.RED, tok[1]);
                                chess.setRandomPlayerNames();
                                chess.setPlayerName(Chess.Side.RED, tok[1]);
                            } else {
                                chess.setRandomPlayerNames();
                            }
                            chess.GameSetUp(true);
                            started = true;
                        } else if (lower.startsWith("move ")) {
                            if (!started) {
                                chess.setRandomPlayerNames();
                                chess.GameSetUp(true);
                                started = true;
                            }
                            String payload = cmd.substring(5).trim().toUpperCase();
                            String[] parts = payload.split("\\s+");
                            if (parts.length == 2 && parts[0].length() == 2 && parts[1].length() == 2) {
                                int fromCol = parts[0].charAt(0) - 'A';
                                int fromRow = Character.getNumericValue(parts[0].charAt(1));
                                int toCol = parts[1].charAt(0) - 'A';
                                int toRow = Character.getNumericValue(parts[1].charAt(1));
                                chess.movePiece(fromRow, fromCol, toRow, toCol);
                            } else {
                                publish("Skipping malformed move: " + cmd);
                            }
                        } else if (lower.equals("undo")) {
                            if (started) chess.undo();
                        } else if (lower.equals("stop")) {
                            publish("Stop encountered in record.");
                            break;
                        } else {
                            publish("Ignoring unknown command: " + cmd);
                        }
                    } catch (Exception ex) {
                        publish("Error: " + ex.getMessage());
                    }

                    // update UI
                    SwingUtilities.invokeLater(() -> {
                        updateBoard();
                        updateStatus();
                        commentLabel.setText("Replaying: " + cmd);
                    });

                    if (chess.isGameOver()) {
                        publish("Game finished during replay. Winner: " + chess.getWinnerName());
                        break;
                    }

                    if (stepMode) {
                        // show modal dialog on EDT and wait for user to continue or cancel
                        try {
                            final int[] result = new int[1];
                            SwingUtilities.invokeAndWait(() -> {
                                result[0] = JOptionPane.showConfirmDialog(ChessUI.this, "Next command?\n" + cmd, "Replay - Step", JOptionPane.OK_CANCEL_OPTION);
                            });
                            if (result[0] != JOptionPane.OK_OPTION) {
                                publish("Replay stopped by user.");
                                break;
                            }
                        } catch (Exception ex) {
                            publish("Step dialog error: " + ex.getMessage());
                            break;
                        }
                    } else {
                        try { Thread.sleep(800); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                // show latest status messages in commentLabel
                String last = chunks.get(chunks.size() - 1);
                commentLabel.setText(last);
            }

            @Override
            protected void done() {
                setInteractiveEnabled(true);
                updateBoard();
                updateStatus();
                if (chess.isGameOver()) {
                    SwingUtilities.invokeLater(() -> showWinDialog());
                } else {
                    JOptionPane.showMessageDialog(ChessUI.this, "Replay finished.", "Replay", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
                    SwingUtilities.invokeLater(ChessUI::new);
    }
}
