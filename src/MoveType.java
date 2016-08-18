
public enum MoveType {
	MOVE, CAPTURE, CHECK, CHECKMATE;
	
	public String toString(){
		return this.name().substring(0,1) + this.name().substring(1).toLowerCase();
	}
}
