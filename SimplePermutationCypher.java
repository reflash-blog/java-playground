// https://studopedia.su/1_15709_shifrovanie-metodom-perestanovki.html

import java.util.Random;
import java.util.*;
import java.io.*;

class Main {
  static SimpleCrypto crypter = new SimpleCrypto();
  
  private static void writeTo(String dirName, String fileName, String row){
    File dir = new File(dirName);
    if(!dir.exists())
      dir.mkdirs();
      
    try(FileWriter fw = new FileWriter(dirName + "\\" + fileName, true);
    BufferedWriter bw = new BufferedWriter(fw);
    PrintWriter out = new PrintWriter(bw))
      {
          out.println(row);
      } catch (IOException e) {
          System.out.println("Exception while handling file " + e);
      }
  }
  
  public static void main(String[] args) {
    Scanner input = new Scanner(System.in);
    System.out.print("Enter the message: ");
    String message = input.nextLine(); // "agsadhhwewhweh hwehewehw hewhwehweh";
    System.out.println("message = " + message);
    input.close();
    
    String key = crypter.key(message);
    System.out.println("key = " + key);
    
    String encoded = crypter.encode(key, message);
    System.out.println("encoded = " + encoded);
    writeTo("keys", "encoded", encoded);
    
    String decoded = crypter.decode(key, encoded);
    System.out.println("decoded = " + decoded);
    writeTo("keys", "decoded", decoded);
    
    if (!message.equals(decoded))
      throw new AssertionError("Initial message and decoded are not equal:" + "message = " + message + "; decoded = " + decoded + ";");
    
    System.out.println("Test passed succesfully");
  }
}

class SimpleCrypto{
  
  // shuffle string
  private String shuffle(String m){
    Random r = new Random();
    // Convert your string into a simple char array:
    char a[] = m.toCharArray();

    // Scramble the letters using the standard Fisher-Yates shuffle, 
    for (int i = 0; i < a.length; i++)
    {
        int j = r.nextInt(a.length);
        // Swap letters
        char temp = a[i]; a[i] = a[j];  a[j] = temp;
    }       

    return new String(a);
  }
  
  // generate key
  public String key(String message){
    int n = message.length() < 10 ? message.length() : 10;
    String key = "";
    for (int i = 0; i < n; i++) key += i;
    return shuffle(key);
  }
  
  /*
    message = asdasd63426436234623462436
    n = 10
    m = 3
    char [m][n]
    block = asdasd6342
            6436234623
            462436
    key = 1502836794
    return = s46d36a64d3242 a6464 36 23 s23
    
  */
  public String encode(String key, String message){
    int n = key.length();
    int m = (int) Math.ceil((double) message.length() / n);
    char[][] block = new char[m][n];
    
    // create block from message
    for (int i = 0; i < m; i++)
      for (int j = 0; j < n; j++)
        block[i][j] = (i*n + j) < message.length() ? 
          message.charAt(i*n + j) : 
          ' ';
    
    System.out.println("Block: ");
    for (int i = 0; i < m; i++) 
      System.out.println(new String(block[i]));
    
    // encode using block columns permutations
    StringBuffer buf = new StringBuffer();
    
    for (char k: key.toCharArray()){
      int i = Character.getNumericValue(k);
      for (int j = 0; j < m; j++)
        buf.append(block[j][i]);
    }
    
    return new String(buf);
  }
  
  /*
    cypher = s46d36a64d3242 a6464 36 23 s23
    n = 10
    m = 3
    char [m][n]
    block = asdasd6342
            6436234623
            462436
            
    key = 1502836794
    return = asdasd63426436234623462436
    
  */
  public String decode(String key, String cipher){
    int n = key.length();
    int m = (int) Math.ceil((double) cipher.length() / n);
    char[][] block = new char[m][n];
    
    // decode into block structure
    int p = 0;
    for (char k: key.toCharArray()){
      int i = Character.getNumericValue(k);
      for (int j = 0; j < m; j++)
        block[j][i] = (p*m + j) < cipher.length() ? 
          cipher.charAt(p*m + j) : 
          ' ';
      p++;
    }
    
    System.out.println("Block: ");
    for (int i = 0; i < m; i++) 
      System.out.println(new String(block[i]));
    
    // decode from block by lines
    StringBuffer buf = new StringBuffer();
    
    for (int i = 0; i < m; i++){
      for (int j = 0; j < n; j++)
        buf.append(block[i][j]);
    }
    
    return new String(buf).replaceAll("\\s+$","");
  }
}