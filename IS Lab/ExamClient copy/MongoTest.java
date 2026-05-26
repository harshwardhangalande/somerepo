import com.mongodb.client.*;
import org.bson.Document;

public class MongoTest {
    public static void main(String[] args) {
        MongoClient client = MongoClients.create(
                "mongodb+srv://dbUser:dbUserPassword@cluster0.igauqwz.mongodb.net/?appName=Cluster0");

        MongoDatabase db = client.getDatabase("ExamDB");
        MongoCollection<Document> col = db.getCollection("questionpapers");

        Document doc = col.find(new Document("key", "10-CS301-3")).first();

        if (doc == null) {
            System.out.println("NOT FOUND");
        } else {
            System.out.println("FOUND:");
            System.out.println(doc.getString("content"));
        }
    }
}