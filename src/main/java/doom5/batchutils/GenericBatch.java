package doom5.batchutils;

public interface GenericBatch {

	public abstract boolean start() throws Exception;
	public abstract boolean process() throws Exception;
	public abstract boolean end() throws Exception;
	public abstract boolean error() throws Exception;
}
