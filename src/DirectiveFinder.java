import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirectiveFinder {
	Pattern placementPattern;
	Pattern commentPattern;
	Pattern movementPattern;
	Pattern castlingPattern;
	Pattern singleContainingPattern;
	Pattern castle;
	Pattern twoCastlesFound;
	Pattern onlyOne;

	public DirectiveFinder() {
		initializePatterns();
	}

	private void initializePatterns() {
		String placement = "^(?<Pattern1>[KRNQBP][ld][a-h][1-8])\\s*$";
		placementPattern = Pattern.compile(placement);
		String movement = "^(?<Movement1>[KRNQBP]?[a-h][1-8][\\-x][a-h][1-8][#\\+]?)\\s+(?<Movement2>[KRNQBP]?[a-h][1-8][\\-x][a-h][1-8][#\\+]?)$";
		movementPattern = Pattern.compile(movement);
		String lineMovement = "\\s*(?<Castle1>O-O-O|O-O)?\\s*(?<Single1>[KRNQBP]?[a-h][1-8][\\-x][a-h][1-8][#+]?)?\\s*(?<Castle2>O-O-O|O-O)?\\s*";
		castlingPattern = Pattern.compile(lineMovement);
		String containingCastle = "(O-O-O|O-O)";
		castle = Pattern.compile(containingCastle);
		String onlyOneMove = "[^KRNQBPa-h]*(?<Movement1>[KRNQBP]?[a-h][1-8][\\-x][a-h][1-8][#\\+]?)\\s*";
		onlyOne = Pattern.compile(onlyOneMove);
		String twoCastles = "([^O\\-]*(O-O-O|O-O)[^O\\-]*){2}\\s*";
		twoCastlesFound = Pattern.compile(twoCastles);
	}

	public boolean isPlacement(String currentLine) {
		Matcher placementMatcher = placementPattern.matcher(currentLine);
		return placementMatcher.find();
	}

	public boolean isMovement(String currentLine, Board board, DirectiveHandler handler) {
		Matcher movementMatcher = movementPattern.matcher(currentLine);
		Matcher oneMove = onlyOne.matcher(currentLine);
		return movementMatcher.find() || (oneMove.find()
				&& (currentLine.contains("#") || isValidSingleMove(oneMove.group("Movement1"), board, handler))
				&& !currentLine.contains("O"));
	}

	private boolean isValidSingleMove(String movement, Board board, DirectiveHandler handler) {
		Position position1 = new Position(handler.getInitialRank(movement, true),
				handler.getInitialFile(movement, true));
		Position position2 = new Position(handler.getSecondaryRank(movement), handler.getSecondaryFile(movement));
		char piece = handler.getPieceChar(movement);
		boolean valid = board.isValid(position1, position2, true, movement, piece);
		board.setStalemateRequired(valid);
		return valid;
	}

	public String getPlacementDirective(String currentLine) {
		Matcher placementMatcher = placementPattern.matcher(currentLine);
		placementMatcher.find();
		return placementMatcher.group("Pattern1");
	}

	public ArrayList<String> getMovementDirectives(String currentLine) {
		Matcher movementMatcher = movementPattern.matcher(currentLine);
		Matcher oneMovementMatcher = onlyOne.matcher(currentLine);
		ArrayList<String> movementDirectives = new ArrayList<String>();
		if (movementMatcher.find()) {
			movementDirectives.add(movementMatcher.group("Movement1"));
			movementDirectives.add(movementMatcher.group("Movement2"));
		} else if (oneMovementMatcher.find()) {
			movementDirectives.add(oneMovementMatcher.group("Movement1"));
		}
		return movementDirectives;
	}

	public String removeComment(String currentLine) {
		return currentLine.substring(0, currentLine.indexOf('/'));
	}

	public boolean containsComment(String currentLine) {
		return (currentLine.contains("//"));
	}

	public boolean containsCastle(String currentLine) {
		Matcher castleM = castle.matcher(currentLine);
		Matcher twoCast = twoCastlesFound.matcher(currentLine);
		return (twoCast.find() || (containsSingleMovement(currentLine) && castleM.find()));
	}

	public boolean containsSingleMovement(String currentLine) {
		Matcher single = onlyOne.matcher(currentLine);
		return single.find();
	}

	public ArrayList<String> getLineAction(String currentLine) {
		ArrayList<String> movement = new ArrayList<String>();
		Matcher single = castlingPattern.matcher(currentLine);
		single.find();
		if (single.group("Castle1") == null) {
			movement.add(single.group("Single1"));
			movement.add(single.group("Castle2"));
		} else if (single.group("Castle2") == null) {
			movement.add(single.group("Castle1"));
			movement.add(single.group("Single1"));
		} else if (single.group("Single1") == null) {
			movement.add(single.group("Castle1"));
			movement.add(single.group("Castle2"));
		}
		return movement;
	}

	public boolean isCastle(String directive) {
		return directive.contains("O");
	}

}
