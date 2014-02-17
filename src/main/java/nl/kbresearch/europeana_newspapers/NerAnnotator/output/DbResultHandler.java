package nl.kbresearch.europeana_newspapers.NerAnnotator.output;

import nl.kbresearch.europeana_newspapers.NerAnnotator.App;
import nl.kbresearch.europeana_newspapers.NerAnnotator.container.ContainerContext;

//import org.jsoup.nodes.Document;
import org.w3c.dom.Document;

import java.sql.*;

public class DbResultHandler implements ResultHandler {

	ContainerContext context;
	String name;

	static Connection dbConn;
	static long count = 1;
	/**
	 * @param context
	 * @param name
	 * @throws SQLException
	 */
	{
		{
			try {
				Class.forName("org.hsqldb.jdbcDriver");
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			dbConn = DriverManager.getConnection("jdbc:hsqldb:file:"
					+ App.getOutputDirectoryRoot().getAbsolutePath()
					+ "/annotations", "SA", "");
			update("CREATE CACHED TABLE IF NOT EXISTS annotations " + "("
					+ "wordId  VARCHAR(10000),"
					+ " originalText  VARCHAR(10000), "
					+ " text           VARCHAR(10000), "
					+ "label          VARCHAR(10000), "
					+ "continuationId VARCHAR(512), "
					+ "originalFile VARCHAR(10000)" + ")"

			);

		}
	}

	public DbResultHandler(final ContainerContext context, final String name)
			throws SQLException {
		this.context = context;
		this.name = name;

	}

	public void startDocument() {
		count++;

	}

	public void startTextBlock() {

	}

	public void newLine(boolean hyphenated) {

	}

	public void addToken(String wordid, String originalContent, String word,
			String label, String continuationid) {

		if (label != null) {

			try {
				synchronized (dbConn) {
					PreparedStatement st = dbConn
							.prepareStatement("INSERT INTO annotations VALUES(?,?,?,?,?,?)");
					st.setString(1, wordid);
					st.setString(2, originalContent);
					st.setString(3, word);
					st.setString(4, label);
					st.setString(5, continuationid);
					st.setString(6, name);
					st.execute();
				}

			} catch (SQLException e) {
				throw new IllegalStateException(
						"Could not add new token to annotations database!", e);
			}

		}

	}

	public void stopTextBlock() {

	}

	public void stopDocument() {

	}

	public void close() {

	}

	public synchronized void update(String expression) throws SQLException {

		Statement st = null;

		st = dbConn.createStatement(); 
		st.executeUpdate(expression); // run the query

	}

	public void globalShutdown() {
		try {
			update("SHUTDOWN COMPACT");
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void setAltoDocument(Document doc) {
		// TODO Auto-generated method stub
		
	}
}
