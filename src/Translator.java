import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Translator {
	LogWriter writer;
	OutputFormatter format;
	DirectiveFinder finder;
	boolean movementBegun = false;
	BufferedReader file = null;
	Board board;
	DirectiveHandler handler;
	UserInterface ui;
	boolean beginWithInteractionMode = false;

	public Translator(String fileName, boolean containedFile) {
		writer = new LogWriter();
		writer.writeToFile("Process: Log file Initialized.");
		if (containedFile) {
			initializeReader(fileName);
		} else {
			writer.writeToFile("Process: You entered no filepath. The program will now revert to Interaction Mode.");
			beginWithInteractionMode = true;
		}
		format = new OutputFormatter();
		finder = new DirectiveFinder();
		handler = new DirectiveHandler();
		board = new Board(writer);
		ui = new UserInterface();
	}

	public void translate() {
		if (!beginWithInteractionMode) {
			translateFile();
		}
		initiateInteractionMode();
		endGame();
		shutdown();
	}
private void initiateInteractionMode(){
	if (!board.isCheckmate() && !board.isInvalidCheckMove() && !board.isStalemate()) {
		writer.writeToFile("----------------------------------");
		writer.writeToFile("Process: Interactive Mode enabled.");
		writer.writeToFile("----------------------------------");
		interactionMode();
	}
}
	public void interactionMode() {
		boolean quit = false;
		if (beginWithInteractionMode) {
			setUpBoard();
		}
		board.writeBoard();
		board.printBoardToConsole();
		int count = 1;
		boolean isWhite = true;
		while (!quit && board.isPlayable() && !board.isStalemate() && !board.isCheckmate()) {
			int piece;
			boolean pieceChosen;
			isWhite = (count % 2 != 0);
			ArrayList<Piece> pieces = board.getAllPossiblePieces(isWhite);
			King currentPlayerKing = (King) board.getTeamKing(isWhite, board.getBoard());
			if (pieces.size() == 0 && !currentPlayerKing.isCheck()) {
				board.setStalemate(true);
			} else if (pieces.size() == 0 && currentPlayerKing.isCheck()) {
				board.setCheckmate(true);
			}
			if (!board.isStalemate() && !board.isCheckmate()) {
				ui.inform(isWhite);
				do {
					pieceChosen = true;
					piece = ui.determinePiece(pieces);
					quit = isQuit(piece);
					if (!quit) {
						ArrayList<Position> possibleMoves = getAllMovesForPiece(pieces, piece, isWhite);
						board.printBoardToConsole();
						int move = ui.determineMove(possibleMoves);
						quit = isQuit(move);
						pieceChosen = !(move == 1);
						if (pieceChosen && !quit)
							getCompleteMovementAndProcess(pieces, piece, possibleMoves, move, isWhite);
					}
				} while (!pieceChosen);
			}
			++count;
			board.setPostMoveChecks();
		}
		board.printBoardToConsole();
	}

	private void getCompleteMovementAndProcess(ArrayList<Piece> pieces, int piece, ArrayList<Position> possibleMoves,
			int move, boolean isWhite) {
		String movement = getCompleteMovement(pieces.get(piece - 1), possibleMoves.get(move - 2));
		if (movement.contains("O")) {
			board.castle(isWhite, movement);
			writer.writeToFile(format.formatCastle(movement, isWhite));
		} else
			processMovement(movement, isWhite);
	}

	private ArrayList<Position> getAllMovesForPiece(ArrayList<Piece> pieces, int piece, boolean isWhite) {
		Piece current = pieces.get(piece - 1);
		ArrayList<Position> possibleMoves = current.getMovement(board.getBoard(),
				(current.getType() == PieceType.PAWN ? false : true));
		possibleMoves = board.getNonCheckMovements(possibleMoves, current,
				(King) board.getTeamKing(current.isWhite(), board.getBoard()));
		if (current.getType() == PieceType.KING || current.getType() == PieceType.ROOK) {
			if (board.isValidCastle("O-O-O", isWhite)
					&& current.getCurrentPosition().equals(board.getRookPosition(isWhite, false)))
				possibleMoves.add(new Position(-1, -1));
			if (board.isValidCastle("O-O", isWhite)
					&& current.getCurrentPosition().equals(board.getRookPosition(isWhite, true)))
				possibleMoves.add(new Position(8, 8));
		}
		return possibleMoves;
	}

	private boolean isQuit(int choice) {
		return choice == 0;
	}

	private void setUpBoard() {
		BufferedReader initializer;
		try {
			FileInputStream inputStream = new FileInputStream("src/BoardInitialization.chess");
			initializer = new BufferedReader(new InputStreamReader(inputStream));
			while (initializer.ready()) {
				processPlacement(initializer.readLine().trim());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void translateFile() {
		try {
			while (file.ready() && !board.isCheckmate() && !board.isInvalidCheckMove()) {
				boolean wasMove = false;
				String currentLine = getCurrentLine().trim();
				if (finder.containsComment(currentLine)) {
					currentLine = finder.removeComment(currentLine).trim();
				}
				if (currentLine.trim().length() > 0) {
					if (finder.isPlacement(currentLine)) {
						processPlacement(currentLine);
					} else if (finder.isMovement(currentLine, board, handler)) {
						ArrayList<String> movements = finder.getMovementDirectives(currentLine);
						if (!board.isCheckmate() && !board.isInvalidCheckMove())
							processMovement(movements.get(0), true);
						if (movements.size() > 1 && !board.isCheckmate() && !board.isInvalidCheckMove())
							processMovement(movements.get(1), false);
						wasMove = true;
					} else if (finder.containsCastle(currentLine) && !board.isCheckmate()) {
						processCastling(currentLine);
						wasMove = true;

					} else {
						writer.writeToFile(format.getIncorrect(currentLine));
					}
				}
				if (wasMove) {
					board.setPostMoveChecks();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeReader(String fileName) {
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(fileName);
			file = new BufferedReader(new InputStreamReader(inputStream));
			writer.writeToFile("Process: Sucessfully opened file [" + fileName + "]");
		} catch (FileNotFoundException e) {
			writer.writeToFile(
					"Error: There was a problem with the file you entered. Reverting to Interaction Mode.");
			beginWithInteractionMode = true;
		}
	}

	private String getCurrentLine() {
		String currentLine = null;
		try {
			currentLine = file.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return currentLine;
	}

	private void processPlacement(String currentLine) {
		if (movementBegun) {
			writer.writeToFile("Warning: Skipping [" + currentLine + "]. Movement has begun.");
		} else {
			String placement = finder.getPlacementDirective(currentLine);
			board.addNewPiece(placement);
			String placement1 = "Placement: Adding [" + placement + "] " + format.formatPlacement(placement);
			writer.writeToFile(placement1);
		}
	}

	private void processMovement(String currentMovement, boolean isFirstMovement) {
		if (!movementBegun) {
			movementBegun = true;
		}

		boolean movementValid = board.movePiece(currentMovement, isFirstMovement);
		if (movementValid) {
			writer.writeToFile(format.formatMovement(currentMovement, isFirstMovement));
			board.writeBoard();
		} else {
			writeMovementError(currentMovement, isFirstMovement);
		}
	}

	private void processCastling(String currentLine) throws Exception {
		ArrayList<String> lineAction = finder.getLineAction(currentLine);
		if (lineAction.get(0) != null && lineAction.get(1) != null) {
			if (finder.containsSingleMovement(currentLine)) {
				if (lineAction.size() == 2) {
					if (finder.isCastle(lineAction.get(0))) {
						if (board.isValidCastle(lineAction.get(0), true)) {
							board.castle(true, lineAction.get(0));
							writer.writeToFile(format.formatCastle(lineAction.get(0), true));
						} else {
							writer.writeToFile("This castle is impossible at this time.");
						}
					} else {
						if (board.movePiece(lineAction.get(0), true)) {
							writer.writeToFile(format.formatMovement(lineAction.get(0), true));
						} else {
							writeMovementError(lineAction.get(0), true);
						}
					}
					if (finder.isCastle(lineAction.get(1))) {
						if (board.isValidCastle(lineAction.get(1), false)) {
							board.castle(false, lineAction.get(1));
							writer.writeToFile(format.formatCastle(lineAction.get(1), false));
						} else {
							writer.writeToFile("This castle is impossible at this time.");
						}
					} else {
						if (board.movePiece(lineAction.get(1), false)) {
							writer.writeToFile(format.formatMovement(lineAction.get(1), false));
						} else {
							writeMovementError(lineAction.get(1), false);
						}
					}
				}
			} else {
				if (board.isValidCastle(lineAction.get(0), true)) {
					board.castle(true, lineAction.get(0));
					writer.writeToFile(format.formatCastle(lineAction.get(0), true));
				} else {
					writer.writeToFile("This castle is impossible at this time.");
				}
				if (board.isValidCastle(lineAction.get(1), false)) {
					board.castle(false, lineAction.get(1));
					writer.writeToFile(format.formatCastle(lineAction.get(1), false));
				} else {
					writer.writeToFile("This castle is impossible at this time.");
				}
			}
		} else {
			writer.writeToFile(format.getIncorrect(currentLine));
		}
	}

	public void shutdown() {
		try {
			writer.writeToFile("Process: Closing Files.");
			if (file != null)
				file.close();
			writer.closeLogFile();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void writeMovementError(String movement, boolean isWhite) {
		Position pos1 = new Position(handler.getInitialRank(movement, true), handler.getInitialFile(movement, true));
		Position pos2 = new Position(handler.getSecondaryRank(movement), handler.getSecondaryFile(movement));
		String s = format.formatInvalidMovement(board, pos1, pos2, isWhite, movement, handler.getPieceChar(movement));
		writer.writeToFile(s);
	}

	private String getCompleteMovement(Piece piece, Position position) {
		String movement;
		if (position.file == -1)
			movement = "O-O-O";
		else if (position.file == 8)
			movement = "O-O";
		else {
			Piece[][] currentBoard = board.getBoard();
			Position piecePosition = piece.getCurrentPosition();
			movement = "" + (piece.getType() == PieceType.PAWN ? "" : piece.getType().getWhiteType())
					+ Character.toLowerCase(ui.getFileLetter(piecePosition.getFile())) + (piecePosition.getRank() + 1);
			movement += (currentBoard[position.getRank()][position.getFile()] == null ? "-" : "x");
			movement += Character.toLowerCase(ui.getFileLetter(position.getFile()));
			movement += (position.getRank() + 1);
			piece.setCurrentPosition(position);
			if (board.isCheck(board.moveSinglePiece(piecePosition, position, board.copyArray(board.getBoard()), piece),
					piece, (King) board.getTeamKing(!piece.isWhite(), currentBoard))) {
				Piece[][] checkBoard = board.moveSinglePiece(piece.getCurrentPosition(), position,
						board.copyArray(currentBoard), piece);
				if (board.isCheckmate(!piece.isWhite(), checkBoard, false)) {
					movement += "#";
				} else {
					movement += "+";
				}
			}
			piece.setCurrentPosition(piecePosition);
		}
		return movement;
	}

	private void endGame() {
		String gameEnding = (board.isStalemate() ? ui.informOfStalemate()
				: board.isCheckmate() ? ui.informOfCheckmate(board.isWinner())
						: board.isInvalidCheckMove() ? ui.informOfInvalid() : "The game has been chosen to end.");
		writer.writeToFile(gameEnding);
		System.out.println(gameEnding);
		if (!board.isCheckmate())
			board.writeBoard();
	}

}
