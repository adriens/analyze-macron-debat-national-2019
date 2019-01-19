package com.github.adriens.macronsentimentanalysis;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;


/*
 * Gson: https://github.com/google/gson
 * Maven info:
 *     groupId: com.google.code.gson
 *     artifactId: gson
 *     version: 2.8.1
 *
 * Once you have compiled or downloaded gson-2.8.1.jar, assuming you have placed it in the
 * same folder as this file (GetSentiment.java), you can compile and run this program at
 * the command line as follows.
 *
 * javac GetSentiment.java -classpath .;gson-2.8.1.jar -encoding UTF-8
 * java -cp .;gson-2.8.1.jar GetSentiment
 */
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;

class Document {

    public String id, language, text, score;

    public Document(String id, String language, String text) {
        this.id = id;
        this.language = language;
        this.text = text;
        this.score = "";
    }

    public String getId() {
        return this.id;
    }

    public String getScore() {
        return this.score;
    }

    public String getText() {
        return this.text;
    }
}

class Documents {

    public List<Document> documents;

    public Documents() {
        this.documents = new ArrayList<Document>();
    }

    public void add(String id, String language, String text) {
        this.documents.add(new Document(id, language, text));
    }
}

public class GetSentiment {

// ***********************************************
// *** Update or verify the following values. ***
// **********************************************
// Replace the accessKey string value with your valid access key.
    static String accessKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

// Replace or verify the region.
// You must use the same region in your REST API call as you used to obtain your access keys.
// For example, if you obtained your access keys from the westus region, replace 
// "westcentralus" in the URI below with "westus".
// NOTE: Free trial access keys are generated in the westcentralus region, so if you are using
// a free trial access key, you should not need to change this region.
    static String host = "https://westcentralus.api.cognitive.microsoft.com";

    static String path = "/text/analytics/v2.0/sentiment";

    public static String GetSentiment(Documents documents) throws Exception {
        String text = new Gson().toJson(documents);
        byte[] encoded_text = text.getBytes("UTF-8");

        URL url = new URL(host + path);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/json");
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", accessKey);
        connection.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.write(encoded_text, 0, encoded_text.length);
        wr.flush();
        wr.close();

        StringBuilder response = new StringBuilder();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        return response.toString();
    }

    public static String prettify(String json_text) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(json_text).getAsJsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);
    }
    
    // http://opennlp.sourceforge.net/models-1.5/
    public static String[] getTokens(String aText) throws Exception {
        if(aText==null){
            return null;
        }
        InputStream inputStream = GetSentiment.class.getResourceAsStream("/en-token.bin");
        TokenizerModel model = new TokenizerModel(inputStream);
        TokenizerME tokenizer = new TokenizerME(model);
        return tokenizer.tokenize(aText);//String[] tokens = tokenizer.tokenize("Baeldung is a Spring Resource.");
  
    }

    public static String[] detectSentence(String paragraph) throws IOException {

        // get models : http://opennlp.sourceforge.net/models-1.5/
        InputStream modelIn = GetSentiment.class.getResourceAsStream("/en-sent.bin");
        SentenceModel model = new SentenceModel(modelIn);

        SentenceDetectorME sdetector = new SentenceDetectorME(model);

        String sentences[] = sdetector.sentDetect(paragraph);
        return sentences;

    }

    // https://docs.microsoft.com/en-us/azure/cognitive-services/text-analytics/how-tos/text-analytics-how-to-sentiment-analysis
    public static void main(String[] args) {
        try {
            
            System.out.println(getTokens("Hell I'm fine").length);
            File file = new File("lettre-aux-francais-macron-2019_en.md");
            String paragraph = FileUtils.readFileToString(file, "UTF-8");
            //System.out.println("Read in: " + paragraph);
            // split text into documents (by sentences)
            String sentences[] = detectSentence(paragraph);
            System.out.println("Nb sentences : <" + sentences.length + ">");
            // analyze each sentence
            Documents documents = new Documents();
            int i = 0;
            System.out.println("Feeding documents...");
            while (i < sentences.length) {
                System.out.println("<" + i + "> : [" + sentences[i] + "]");
                documents.add((i + 1) + "", "en", sentences[i]);
                i++;
            }
            System.out.println("Documents prepared.");
            System.out.println("Getting sentiments from MS Azure services...");
            String response = GetSentiment(documents);
            System.out.println(prettify(response));

            //
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Documents docs = gson.fromJson(response, Documents.class);
            int j = 0;
            String fileName = "sentiments-scores.csv";

            CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
            FileWriter fileWriter = new FileWriter(fileName);
            CSVPrinter csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);

            while (j < docs.documents.size()) {
                Document lDoc = docs.documents.get(j);
                System.out.println("Score for sentence <" + lDoc.id + "> : <" + lDoc.score + ">");

                List docDataRecord = new ArrayList();
                docDataRecord.add(lDoc.id);
                docDataRecord.add(lDoc.score);
                docDataRecord.add(sentences[j].replace("\n", " ").replace("\r", ""));
                docDataRecord.add(getTokens(sentences[j]).length);
                csvFilePrinter.printRecord(docDataRecord);
                j++;
            }

            fileWriter.flush();

            fileWriter.close();

            csvFilePrinter.close();
            //System.out.println("Nb json docs : " + docs.);
            /*
            Documents documents = new Documents ();
            documents.add("1", "en", "I really enjoy the new XBox One S. It has a clean look, it has 4K/HDR resolution and it is affordable.");
            documents.add ("2", "en", "I hate people !");
            documents.add("3","fr", "J'aime nager en mer.");
            documents.add("4","fr", "Je détester les cons");

            String response = GetSentiment (documents);
            System.out.println (prettify (response));
             */
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
