import org.junit.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.lang.String;
import static org.junit.Assert.*;


public class ProjectTest {
    private String[] names = {"Tiger", "Dragon", "Phoenix", "Panda", "Monkey", "Crane", "Leopard", "Wolf", "Peter", "Andrew", "James", "John","Philip","Bartholomew","Matthew","Thomas","Semaj","Simon","Thaddeus","Amadeus", "Steve404"};
    private Chess chess;
    private final InputStream systemIn = System.in;
    private final PrintStream systemOut = System.out;

    private ByteArrayOutputStream outContent;

    @Before
    public void setUpStreams() throws IOException {
        chess = new Chess();
        chess.GameSetUp(true);
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setIn(systemIn);
        System.setOut(systemOut);
    }

    // checkWinConidition(), isGameOver(), getWinnerName(), by entering opponent's den
    // pass
    @Test
    public void test_win_condition1() {
        chess.movePiece(7, 4, 7, 3); // Red leopard moves
        chess.movePiece(3, 4, 2, 4); // Blue wolf moves
        chess.movePiece(7, 3, 6, 3); // Red leopard moves
        chess.movePiece(3, 6, 4, 6); // Blue elephant moves
        chess.movePiece(6, 3, 5, 3); // Red leopard moves
        chess.movePiece(2, 4, 3, 4); // Blue wolf moves
        chess.movePiece(5, 3, 4, 3); // Red leopard moves
        chess.movePiece(3, 4, 2, 4); // Blue wolf moves
        chess.movePiece(4, 3, 3, 3); // Red leopard moves
        chess.movePiece(2, 1, 2, 2); // Blue dog moves
        chess.movePiece(3, 3, 2, 3); // Red leopard moves, into trap
        chess.movePiece(2, 2, 2, 1); // Blue dog moves
        assertTrue(chess.movePiece(2, 3, 1, 3)); // Red leopard moves into blue den
        assertTrue(chess.isGameOver()); 
        assertEquals(chess.board[1][3].piece.owner.side, Chess.Side.RED);
        String out = outContent.toString();
        assertTrue(out.contains(" (Red) wins"));
    }

    // Capture + count remaining pieces
    // pass
    @Test
    public void test_capture_lower_and_same_rank() {
        chess.movePiece(7, 2, 7, 3); // Red wolf moves
        chess.movePiece(3, 2, 3, 3); // Blue leopard moves
        chess.movePiece(7, 3, 6, 3); // Red wolf moves
        chess.movePiece(3, 3, 4, 3); // Blue leopard moves
        chess.movePiece(6, 3, 5, 3); // Red wolf moves
        assertTrue(chess.movePiece(4, 3, 5, 3)); // Blue leopard captures red wolf
        assertEquals(7, chess.countRemainingPieces(Chess.Side.RED));

        chess.movePiece(7, 4, 7, 3); // Red leopard moves
        chess.movePiece(5, 3, 6, 3); // Blue leopard moves
        assertTrue(chess.movePiece(7, 3, 6, 3)); // Blue leopard captures red wolf
        assertEquals(7, chess.countRemainingPieces(Chess.Side.BLUE));
    }

    // Capture + count remaining pieces
    // pass
    @Test
    public void test_capture_higher_rank() {
        chess.movePiece(7, 2, 7, 3); // Red wolf moves
        chess.movePiece(3, 2, 3, 3); // Blue leopard moves
        chess.movePiece(7, 3, 6, 3); // Red wolf moves
        chess.movePiece(3, 3, 4, 3); // Blue leopard moves
        chess.movePiece(6, 3, 5, 3); // Red wolf moves
        chess.movePiece(3, 0, 3, 1); // Blue rat moves

        assertFalse(chess.movePiece(5, 3, 4, 3)); // Red wolf captures blue leopard fails
        String out = outContent.toString();
        assertTrue(out.contains("Cannot capture: your piece's rank is too low or not allowed by special rules."));
    }

    // Capture + count remaining pieces
    // pass
    @Test
    public void test_rat_capture_elephant() {
        chess.movePiece(7, 6, 6, 6); // Red rat moves
        chess.movePiece(3, 6, 4, 6); // Blue elephant moves
        chess.movePiece(6, 6, 5, 6); // Red rat moves
        assertFalse(chess.movePiece(4, 6, 5, 6)); //Blue elephant captures red rat fails
        String out = outContent.toString();
        assertTrue(out.contains("Elephant cannot capture rat."));
        chess.movePiece(3, 0, 4, 0); // Blue rat moves

        assertTrue(chess.movePiece(5, 6, 4, 6)); // Red rat captures blue elephant
        assertEquals(7, chess.countRemainingPieces(Chess.Side.BLUE));
    }

        // Capture + count remaining pieces
    // pass
    @Test
    public void test_rat_capture_rat() {
        chess.movePiece(7, 6, 7, 5); // Red rat moves
        chess.movePiece(3, 0, 4, 0); // Blue rat moves
        chess.movePiece(7, 5, 6, 5); // Red rat moves
        chess.movePiece(4, 0, 4, 1); // Blue rat moves
        chess.movePiece(6, 5, 6, 4); // Red rat moves
        chess.movePiece(4, 1, 4, 2); // Blue rat moves
        chess.movePiece(6, 4, 5, 4); // Red rat moves
        chess.movePiece(4, 2, 4, 3); // Blue rat moves
        chess.movePiece(5, 4, 4, 4); // Red rat moves

        assertFalse(chess.movePiece(4, 3, 4, 4)); // Blue rat (land) captures red rat (river) fails
        String out = outContent.toString();
        assertTrue(out.contains("Rat can only capture if both pieces are in the river or both are on land."));

        chess.movePiece(4, 3, 3, 3); // Blue rat moves
        chess.movePiece(4, 4, 4, 3); // Red rat moves
        assertTrue(chess.movePiece(3, 3, 4, 3)); // Blue rat captures red rat, both on land
        assertEquals(7, chess.countRemainingPieces(Chess.Side.RED));
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
    }

    // Capture + count remaining pieces
    // pass
    @Test
    public void test_river_rat_capture_land_elephant() {
        chess.movePiece(7, 6, 7, 5); // Red rat moves
        chess.movePiece(3, 6, 4, 6); // Blue elephant moves
        chess.movePiece(7, 5, 6, 5); // Red rat moves, into river
        chess.movePiece(4, 6, 5, 6); // Blue elephant moves
        chess.movePiece(6, 5, 5, 5); // Red rat moves
        chess.movePiece(3, 4, 3, 5); // Blue wolf moves

        assertFalse(chess.movePiece(5, 5, 5, 6)); // Red rat (river) captures blue elephant (land) fails
        String out = outContent.toString();
        assertTrue(out.contains("Rat can only capture elephant if rat is on land."));
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
    }

    // Capture + count remaining pieces
    // pass?
    @Test
    public void test_capture_inside_trap() {
        chess.movePiece(7, 4, 7, 3); // Red leopard moves
        chess.movePiece(3, 4, 2, 4); // Blue wolf moves
        chess.movePiece(7, 3, 6, 3); // Red leopard moves
        chess.movePiece(3, 6, 4, 6); // Blue elephant moves
        chess.movePiece(6, 3, 5, 3); // Red leopard moves
        chess.movePiece(2, 4, 3, 4); // Blue wolf moves
        chess.movePiece(5, 3, 4, 3); // Red leopard moves
        chess.movePiece(3, 4, 2, 4); // Blue wolf moves
        chess.movePiece(4, 3, 3, 3); // Red leopard moves
        chess.movePiece(2, 1, 2, 2); // Blue dog moves
        chess.movePiece(3, 3, 2, 3); // Red leopard moves, into trap
        assertTrue(chess.movePiece(2, 2, 2, 3)); // Blue dog captures red leopard in trap
        assertEquals(7, chess.countRemainingPieces(Chess.Side.RED));
    }

    // movePiece()
    // pass
    @Test
    public void test_valid_movePiece() {
        assertTrue(chess.movePiece(8, 1, 7, 1)); // move A7 A6
        assertNotSame(chess.getCurrentPlayer().side, Chess.Side.RED); // Should switch to blue
        assertSame(chess.getCurrentPlayer().side, Chess.Side.BLUE); // Should switch to blue
    }

    // movePiece()
    // pass
    @Test
    public void test_row0() {
        assertFalse(chess.movePiece(7, 2, 0, 2));
        String out = outContent.toString();
        assertTrue(out.contains("Cannot move into action row."));
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side); // Still red's turn
    }

    // movePiece()
    // pass
    @Test
    public void test_move_from_empty_cell() {
        assertFalse(chess.movePiece(7, 1, 7, 2)); // Move from empty cell
        String out = outContent.toString();
        assertTrue(out.contains("No piece at the source position."));
    }

    // movePiece()
    // pass
    @Test
    public void test_move_opponents_piece() {
        assertFalse(chess.movePiece(1, 0, 2, 0)); // Try to move blue lion as red
        String out = outContent.toString();
        assertTrue(out.contains("You can only move your own pieces.")); 
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
    }

    // movePiece()
    // pass
    @Test
    public void test_move_more_than_one_square() {
        assertFalse(chess.movePiece(7, 2, 5, 2)); // Try to move 2 squares
        String out = outContent.toString();
        assertTrue(out.contains("Pieces can only move vertically or horizontally by one square, except tiger/lion can jump river.")); 
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
    }

    // movePiece()
    // pass
    @Test
    public void test_rat_into_river() {
        assertTrue(chess.movePiece(7, 6, 6, 6)); // Move red rat up
        assertEquals(Chess.Side.BLUE, chess.getCurrentPlayer().side);
        chess.movePiece(3, 4, 3, 3); // Move blue piece
        assertTrue(chess.movePiece(6, 6, 6, 5)); // Move red rat into river
    }

    // movePiece()
    // pass
    @Test
    public void test_non_rat_into_river() {
        assertFalse(chess.movePiece(7, 4, 6, 4)); // Try to move leopard into river
        String out = outContent.toString();
        assertTrue(out.contains("Only the rat can move into or out of water.")); 
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
    }

    // movePiece()
    // pass
    @Test
    public void test_move_to_cell_with_own_piece() {
        assertTrue(chess.movePiece(7, 2, 7, 1));
        assertTrue(chess.movePiece(3, 0, 3, 1));
        assertFalse(chess.movePiece(7, 1, 8, 1)); // Try to move wolf into cat
        String out = outContent.toString();
        assertTrue(out.contains("Cannot move to a cell occupied by your own piece.")); 
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
    }

    // movePiece()
    // pass
    @Test
    public void test_no_piece_at_source() {
        assertFalse(chess.movePiece(8, 2, 8, 3));
        assertFalse(chess.movePiece(8, 2, 8, 1));
        String out = outContent.toString();
        assertTrue(out.contains("No piece at the source position.")); 
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
    }

    // movePiece()
    // pass
    @Test
    public void test_vertical_jumping_river() {
        chess.movePiece(8, 1, 8, 2); // Red moves
        chess.movePiece(2, 5, 2, 4); // Blue moves
        chess.movePiece(9, 0, 8, 0); // Red Tiger moves
        chess.movePiece(1, 6, 2, 6); // Blue Tiger moves
        chess.movePiece(8, 0, 8, 1); // Red Tiger moves
        chess.movePiece(2, 6, 2, 5); // Blue Tiger moves
        chess.movePiece(8, 1, 7, 1); // Red Tiger moves
        chess.movePiece(2, 5, 3, 5); // Blue Tiger moves
     
        assertTrue(chess.movePiece(7, 1, 3, 1)); // Red Tiger jumps
        assertEquals(Chess.Side.BLUE, chess.getCurrentPlayer().side);
        assertTrue(chess.movePiece(3, 5, 7, 5)); // Blue tiger jumps
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
    }

    // movePiece()
    // pass
    @Test
    public void test_horizontal_jumping_river() {
        chess.movePiece(7, 6, 7, 5); // Red rat moves
        chess.movePiece(3, 6, 3, 5); // Blue elephant moves
        chess.movePiece(9, 6, 8, 6); // Red lion moves
        chess.movePiece(1, 6, 2, 6); // Blue tiger moves
        chess.movePiece(8, 6, 7, 6); // Red lion moves
        chess.movePiece(2, 6, 3, 6); // Blue tiger moves
        chess.movePiece(7, 6, 6, 6); // Red lion moves
        chess.movePiece(3, 6, 4, 6); // Blue tiger moves

        assertTrue(chess.movePiece(6, 6, 6, 3)); // Red lion jumps
        assertEquals(Chess.Side.BLUE, chess.getCurrentPlayer().side);
        assertTrue(chess.movePiece(4, 6, 4, 3)); // Blue tiger jumps
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
    }

    // movePiece()
    // pass
    @Test
    public void test_rat_blocks_jumping() {
        chess.movePiece(7, 6, 7, 5); // Red rat moves
        chess.movePiece(3, 6, 3, 5); // Blue elephant moves
        chess.movePiece(9, 6, 8, 6); // Red lion moves
        chess.movePiece(1, 6, 2, 6); // Blue tiger moves
        chess.movePiece(8, 6, 7, 6); // Red lion moves
        chess.movePiece(2, 6, 3, 6); // Blue tiger moves
        chess.movePiece(7, 6, 6, 6); // Red lion moves
        chess.movePiece(3, 6, 4, 6); // Blue tiger moves
        chess.movePiece(7, 5, 6, 5); // Red rat moves into river to block

        assertTrue(chess.movePiece(4, 6, 4, 3)); // Blue tiger jumps horizontally
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);

        assertFalse(chess.movePiece(6, 6, 6, 3)); // Red lion jumps, blocked by rat
        String out = outContent.toString();
        assertTrue(out.contains("Cannot jump: rat blocks the river."));
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
        
    }

    // undo()
    // pass
    @Test
    public void test_no_moves_to_undo() throws IOException {
        assertFalse(chess.undo());
        String out = outContent.toString();
        assertTrue(out.contains("No moves to undo."));
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
        assertEquals(3, chess.redUndoRemaining);

        chess.movePiece(7, 4, 7, 3); // Red leopard moves
        assertEquals(Chess.Side.BLUE, chess.getCurrentPlayer().side);
        assertTrue(chess.undo()); // Undo first move, back to red
        String out2 = outContent.toString();
        assertTrue(out2.contains("Undo successful."));
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
        assertEquals(2, chess.redUndoRemaining);
        assertFalse(chess.undo()); //Undo again, no moves to undo
        String out3 = outContent.toString();
        assertTrue(out3.contains("No moves to undo."));
    }

    // undo()
    // pass
    @Test
    public void test_undo_limit() throws IOException {
        for (int i = 0; i < 3; i++) {
            chess.movePiece(7, 2, 7, 3); // Red wolf moves
            chess.movePiece(3, 4, 3, 5); // Blue wolf moves
            assertTrue(chess.undo()); // Undo red move
            assertEquals(2-i, chess.blueUndoRemaining);
            assertTrue(chess.undo()); // Undo blue move
            assertEquals(2-i, chess.redUndoRemaining);
        }
        chess.movePiece(7, 4, 7, 5); // Red leopard moves
        assertFalse(chess.undo()); // Undo red move exceeds limit
        String out = outContent.toString();
        assertTrue(out.contains("No undos remaining for"));
    }

    // logEvent
    // pass 
    @Test
    public void test_log_event() throws IOException {
        String currentLogFileName = null;
        currentLogFileName = "game_log_999.record"; // Just for testing, will remove after finishing the test
        java.io.FileWriter fw = new java.io.FileWriter(currentLogFileName, false);

        fw.write("move A7 A6\n");
        fw.close();

        Path logPath = Paths.get(currentLogFileName);
        assertTrue(Files.exists(logPath));
        assertTrue(Files.readString(logPath).contains("move A7 A6"));

        Files.deleteIfExists(logPath);
    }

    // getNextLogNumber
    // pass
    @Test
    public void test_log_num() throws IOException {
        Path recordDir = Paths.get("record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_900.record");
        Files.writeString(file, "start Simon Thaddeus\\n");

        chess.GameSetUp(false); 

        File[] files = recordDir.toFile().listFiles();
        boolean foundNext = Arrays.stream(files)
                            .anyMatch(f -> f.getName().equals("game_log_901.record"));
        assertTrue(foundNext);

        Files.deleteIfExists(file);
        Files.deleteIfExists(recordDir.resolve("game_log_901.record")); //delete test file
    }

    // saveGameToJungle
    // pass
    @Test
    public void test_save() throws IOException {
        Path dir = Paths.get("jungle");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        Path file = dir.resolve("test_save.jungle");
        chess.GameSetUp(true);
        int filenum = chess.getNextLogNumber(dir.toFile(), ".jungle");
        chess.setPlayerName(Chess.Side.BLUE, "Panda");
        chess.setPlayerName(Chess.Side.RED, "John");
        chess.saveGameToJungle();
        Files.writeString(file, "player RED John\r\n" + 
                        "player BLUE Panda\r\n" + 
                        "current RED\r\n" + 
                        "undos RED 3\r\n" + 
                        "undos BLUE 3\r\n" + 
                        "piece 1 0 BLUE 7\r\n" + 
                        "piece 1 6 BLUE 6\r\n" + 
                        "piece 2 1 BLUE 3\r\n" + 
                        "piece 2 5 BLUE 2\r\n" + 
                        "piece 3 0 BLUE 1\r\n" + 
                        "piece 3 2 BLUE 5\r\n" + 
                        "piece 3 4 BLUE 4\r\n" + 
                        "piece 3 6 BLUE 8\r\n" +
                        "piece 7 0 RED 8\r\n" + 
                        "piece 7 2 RED 4\r\n" + 
                        "piece 7 4 RED 5\r\n" + 
                        "piece 7 6 RED 1\r\n" + 
                        "piece 8 1 RED 2\r\n" + 
                        "piece 8 5 RED 3\r\n" + 
                        "piece 9 0 RED 6\r\n" + 
                        "piece 9 6 RED 7\r\n");
        assertTrue(Files.exists(file));
        assertTrue(Files.mismatch(file, dir.resolve("game_save_"+filenum+".jungle"))==-1);

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir.resolve("game_save_"+filenum+".jungle"));
    }
    
/********************************************************************************
************************** Tests for ChessConUI *********************************
********************************************************************************/

    // main cmd, startGame() input name
    // pass
    @Test
    public void test_unknown_cmd() throws IOException {
        String input = "invalid\nstart\n\n\nstop\nexit\n";  //unknown, start, empty name, empty name, stop, exit
        Path dir = Paths.get("record");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertTrue(out.contains("Unknown command."));
        assertTrue(out.contains("Enter Red player name (leave empty for random): "));
        assertTrue(out.contains("Enter Blue player name (leave empty for random): "));
        assertTrue(out.contains("Game stopped."));
        assertTrue(out.contains("Exiting..."));

        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // main cmd
    // pass
    @Test
    public void test_space_case_insensitive() {
        String input = " ExIt \n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertFalse(out.contains("Unknown command."));
        assertTrue(out.contains("Exiting..."));
    }

    // startGame() assign names
    // pass
    @Test
    public void test_both_names_assigned() throws IOException {
        String input = "start\nAlex\nAlexander\nstop\nexit\n";
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertTrue(out.contains("Alex (RED)"));
        assertTrue(out.contains("Alexander (BLUE)"));
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // startGame() assign names
    // pass
    @Test
    public void test_red_name_only() throws IOException {
        String input = "start\nPete\n\nstop\nexit\n";
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertTrue(out.contains("Pete (RED)"));
        boolean check=false;
        for (String name : names) {
            if(out.contains(name+" (BLUE)")){check=true;break;}
        }
        assertTrue(check);
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // startGame() assign names
    // pass
    @Test
    public void test_blue_name_only() throws IOException {
        String input = "start\n\nAmy\nstop\nexit\n";
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertTrue(out.contains("Amy (BLUE)"));
        boolean check=false;
        for (String name : names) {
            if(out.contains(name+" (RED)")){check=true;break;}
        }
        assertTrue(check);
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // startGame() assign names
    // pass
    @Test
    public void test_both_random_names() throws IOException {
        String input = "start\n\n\nstop\nexit\n";
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        boolean check1=false;
        boolean check2=false;
        for (String name : names) {
            if(out.contains(name+" (RED)"))check1=true;
            if(out.contains(name+" (BLUE)"))check2=true;
            if(check1&&check2)break;
        }
        assertTrue(check1);
        assertTrue(check2);
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // startGame() cmd
    // pass
    @Test
    public void test_invalid_move_stationary() throws IOException {
        String input = "start\n\n\nmove a7 a7\nstop\nexit\n";
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertTrue(out.contains("Pieces can only move vertically or horizontally by one square, except tiger/lion can jump river."));
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // startGame() cmd
    // pass
    @Test
    public void test_invalid_move_format() throws IOException {
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        String input = "start\n\n\nmove a70 a6 a5\nstop\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertTrue(out.contains("Invalid move format. Example: move A7 A6"));
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // startGame() cmd
    // pass
    @Test
    public void test_invalid_move_input() throws IOException {
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        String input = "start\n\n\nmove aa aa\nstop\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertTrue(out.contains("Invalid move input. Example: move A7 A6"));
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // startGame() cmd
    // pass
    @Test
    public void test_move_any_case() throws IOException {
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        String input = "start\n\n\nmove a7 A6\nstop\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertFalse(out.contains("Unknown command."));
        boolean check=false;
        for (String name : names) {
            if(out.contains("Current player: "+name+" (BLUE)")){check=true;break;}
        }
        assertTrue(check);
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
        
    }

    // startGame() cmd
    // pass
    @Test
    public void test_move_undo() throws IOException {
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        String input = "start\n\n\nmove a7 A6\nundo\nstop\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertFalse(out.contains("Unknown command."));
        boolean check=false;
        for (String name : names) {
            if(out.contains("Current player: "+name+" (RED)")){check=true;break;}
        }
        assertTrue(check);
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
        
    }

    // startGame() cmd
    // pass
    @Test
    public void test_move_extra_spaces() throws IOException {
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        Chess chess = new Chess();
        String input = "start\n\n\nmove  A7  A6\nstop\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertFalse(out.contains("Invalid move format"));
        assertEquals(Chess.Side.RED, chess.getCurrentPlayer().side);
        boolean check=false;
        for (String name : names) {
            if(out.contains("Current player: "+name+" (BLUE)")){check=true;break;}
        }
        assertTrue(check);
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // startGame() cmd
    // pass
    @Test
    public void test_move_invalid_col() throws IOException {
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        String input = "start\n\n\nmove H7 A6\nstop\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertTrue(out.contains("Column must be A-G."));
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // startGame() cmd
    // pass
    @Test
    public void test_save_cmd() throws IOException {
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");
        Path dir1 = Paths.get("jungle");
        int filenum1 = chess.getNextLogNumber(dir1.toFile(), ".jungle");

        String input = "start\n\n\nmove A7 A6\nsave\nstop\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();      
        assertTrue(out.contains("Game saved"));
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
        Files.deleteIfExists(dir1.resolve("game_save_"+filenum1+".jungle"));
    }

    // startGame() cmd
    // pass
    @Test
    public void test_stop_cmd() throws IOException {
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");

        String input = "start\n\n\nstop\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertTrue(out.contains("Game stopped."));
        assertTrue(out.contains("Enter command: start | load | replay | exit"));
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // startGame() cmd
    // pass
    @Test
    public void test_unknown_game_cmd() throws IOException {
        Path dir = Paths.get("record");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".record");
        
        String input = "start\n\n\ninvalid\nstop\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();
        assertTrue(out.contains("Unknown command."));
        Files.deleteIfExists(dir.resolve("game_log_"+filenum+".record"));
    }

    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_invalid_cmd() throws IOException {
        String input = "replay\nabc\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);
        String out = outContent.toString();

        assertTrue(out.contains("Invalid record number."));
    }

    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_from_file_empty() throws IOException {
        Path recordDir = Paths.get("record");
        int filenum = chess.getNextLogNumber(recordDir.toFile(), ".record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_"+filenum+".record");
        // Ensure the file exists but is empty
        Files.write(file, new byte[0]);

        String input = "replay\n"+filenum+"\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("Enter record number (e.g. 1, 11), or leave empty to go back to previous page: "));
        assertTrue(out.contains("No commands found in file."));

        Files.deleteIfExists(file); //delete test file
    }

    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_from_file_no_stop() throws IOException {
        Path recordDir = Paths.get("record");
        int filenum = chess.getNextLogNumber(recordDir.toFile(), ".record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_"+filenum+".record");
        Files.writeString(file, "stop");

        String input = "replay\n"+filenum+"\nauto\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("Stop command encountered in record."));

        Files.deleteIfExists(file); //delete test file
    }

    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_from_file_no_undo() throws IOException {
        Path recordDir = Paths.get("record");
        int filenum = chess.getNextLogNumber(recordDir.toFile(), ".record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_"+filenum+".record");
        Files.writeString(file, "undo");

        String input = "replay\n"+filenum+"\nauto\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("Ignoring undo before game start."));

        Files.deleteIfExists(file); //delete test file
    }

    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_from_file_unknown() throws IOException {
        Path recordDir = Paths.get("record");
        int filenum = chess.getNextLogNumber(recordDir.toFile(), ".record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_"+filenum+".record");
        Files.writeString(file, "start\nabc");

        String input = "replay\n"+filenum+"\nauto\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("Unknown command in record, ignoring: "));

        Files.deleteIfExists(file); //delete test file
    }

    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_from_file_no_name() throws IOException {
        Path recordDir = Paths.get("record");
        int filenum = chess.getNextLogNumber(recordDir.toFile(), ".record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_"+filenum+".record");
        Files.writeString(file, "start ab\nmove A7 Aa");

        String input = "replay\n"+filenum+"\nauto\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("Skipping malformed move: "));

        Files.deleteIfExists(file); //delete test file
    }

    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_from_file_no_move() throws IOException {
        Path recordDir = Paths.get("record");
        int filenum = chess.getNextLogNumber(recordDir.toFile(), ".record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_"+filenum+".record");
        Files.writeString(file, "move A7 A6");

        String input = "replay\n"+filenum+"\nauto\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("No explicit start found in record - starting game with random names."));

        Files.deleteIfExists(file); //delete test file
    }

    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_from_file_cancel() throws IOException {
        Path recordDir = Paths.get("record");
        int filenum = chess.getNextLogNumber(recordDir.toFile(), ".record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_"+filenum+".record");
        Files.writeString(file, "start Simon Thaddeus\nmove E7 D7\nmove E3 E2");

        String input = "replay\n\nreplay\n"+filenum+"\ncancel\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("No record number provided."));
        assertTrue(out.contains("Enter record number (e.g. 1, 11), or leave empty to go back to previous page: "));
        assertTrue(out.contains("Loaded "));
        assertTrue(out.contains("Replay mode? (auto | step | cancel):"));
        assertTrue(out.contains("Load cancelled."));

        Files.deleteIfExists(file); //delete test file
    }

    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_from_file_step_cancel() throws IOException {
        Path recordDir = Paths.get("record");
        int filenum = chess.getNextLogNumber(recordDir.toFile(), ".record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_"+filenum+".record");
        Files.writeString(file, "start Simon Thaddeus\nmove E7 D7\nmove E3 E2");

        String input = "replay\n"+filenum+"\nstep\n\n\ncancel\nexit\n"; //only load 2 steps then cancel
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("Enter record number (e.g. 1, 11), or leave empty to go back to previous page: "));
        assertTrue(out.contains("Loaded "));
        assertTrue(out.contains("Replay mode? (auto | step | cancel):"));
        assertTrue(out.contains("Press Enter to continue (or 'cancel' to stop replay): "));
        assertTrue(out.contains("Replay cancelled by user."));

        Files.deleteIfExists(file); //delete test file
    }

    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_from_file_unknown_auto_no_winner() throws IOException {
        Path recordDir = Paths.get("record");
        int filenum = chess.getNextLogNumber(recordDir.toFile(), ".record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_"+filenum+".record");
        Files.writeString(file, "start Simon Thaddeus\nmove E7 D7\nmove E3 E2");

        String input = "replay\n"+filenum+"\nunknown\nauto\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("Enter record number (e.g. 1, 11), or leave empty to go back to previous page: "));
        assertTrue(out.contains("Replay mode? (auto | step | cancel):"));
        assertTrue(out.contains("Enter 'auto', 'step', or 'cancel'."));
        assertFalse(out.contains("Press Enter to continue (or 'cancel' to stop replay): ")); //should enter auto mode
        assertTrue(out.contains("Replay finished. Game not ended or no winner."));

        Files.deleteIfExists(file); //delete test file
    }
    
    // loadFromFile() cmd
    // pass
    @Test
    public void test_load_from_file_with_winner() throws IOException {
        Path recordDir = Paths.get("record");
        int filenum = chess.getNextLogNumber(recordDir.toFile(), ".record");
        Files.createDirectories(recordDir);
        Path file = recordDir.resolve("game_log_"+filenum+".record");
        Files.writeString(file, "start Simon Thaddeus\r\n" + //
                        "move E7 D7\r\n" + //
                        "move E3 E2\r\n" + //
                        "move D7 D6\r\n" + //
                        "move G3 G4\r\n" + //
                        "move D6 D5\r\n" + //
                        "move G1 G2\r\n" + //
                        "move D5 D4\r\n" + //
                        "move G4 G5\r\n" + //
                        "move D4 D3\r\n" + //
                        "move E2 E3\r\n" + //
                        "move D3 D2\r\n" + //
                        "move E3 F3\r\n" + //
                        "move C7 D7\r\n" + //
                        "move F2 E2\r\n" + //
                        "move D2 D1\r\n" + //
                        "Simon (Red) wins by entering Blue's den!");

        String input = "replay\n"+filenum+"\nstep\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("Enter record number (e.g. 1, 11), or leave empty to go back to previous page: "));
        assertTrue(out.contains("Replay mode? (auto | step | cancel):"));
        assertTrue(out.contains("Press Enter to continue (or 'cancel' to stop replay): "));
        assertTrue(out.contains("Winner:"));

        Files.deleteIfExists(file); //delete test file
    }

    //loadSavedGame() cmd
    // pass
    @Test
    public void test_load_saved_game() throws IOException {
        Path dir = Paths.get("jungle");
        int filenum = chess.getNextLogNumber(dir.toFile(), ".jungle");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        Path file = dir.resolve("game_save_"+filenum+".jungle");
        Files.writeString(file, "player RED John\r\n" + 
                        "player BLUE Panda\r\n" + 
                        "current RED\r\n" + 
                        "undos RED 3\r\n" + 
                        "undos BLUE 3\r\n" + 
                        "piece 1 0 BLUE 7\r\n" + 
                        "piece 1 6 BLUE 6\r\n" + 
                        "piece 2 1 BLUE 3\r\n" + 
                        "piece 2 5 BLUE 2\r\n" + 
                        "piece 3 0 BLUE 1\r\n" + 
                        "piece 3 2 BLUE 5\r\n" + 
                        "piece 3 4 BLUE 4\r\n" + 
                        "piece 3 6 BLUE 8\r\n" +
                        "piece 7 0 RED 8\r\n" + 
                        "piece 7 2 RED 4\r\n" + 
                        "piece 7 4 RED 5\r\n" + 
                        "piece 7 6 RED 1\r\n" + 
                        "piece 8 1 RED 2\r\n" + 
                        "piece 8 5 RED 3\r\n" + 
                        "piece 9 0 RED 6\r\n" + 
                        "piece 9 6 RED 7");

        String input = "load\n"+filenum+"\nstop\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("Enter save id (e.g. 1), or leave empty to go back to previous page: "));
        assertTrue(out.contains("Save loaded. You can now continue the game."));


        Files.deleteIfExists(file); //delete test file
    }

    //loadSavedGame() cmd
    // pass
    @Test
    public void test_load_saved_unknown() throws IOException {
        String input = "load\na\nload\n900\nload\n\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        ChessConUI.main(new String[0]);

        String out = outContent.toString();        
        assertTrue(out.contains("Failed to load save."));
        assertTrue(out.contains("Please provide integer only."));
        assertTrue(out.contains("No save id provided."));

    }
}