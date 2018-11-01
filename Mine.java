import java.util.*;
import java.io.*;
import java.math.*;

public class Mine {


  //Number of transactions
  private static int numTransactions;
  //Number of inputs
  private static int numInputs;
  //Number of outputs
  private static int numOutputs;
  //Arraylist to store all transactions that are used in the order they arrived
  private static ArrayList<String> transactionsInTransactionOrder;
  //ArrayList to store all weighted transaction fees
  private static ArrayList<Integer> weightedTransactionFees;
  //ArrayList to store total ins + total outs for each transaction
  private static ArrayList<Integer> transactionInputsPlusOutputs;
  //ArrayList for final transactions after optimization
  private static ArrayList<String> finalTransactions;
  //Total transaction fees
  private static int totalTransactionFees;

  private static void init() {
    numTransactions                 = 0;
    numInputs                       = 0;
    numOutputs                      = 0;
    totalTransactionFees            = 0;
    transactionsInTransactionOrder  = new ArrayList<String>();
    weightedTransactionFees         = new ArrayList<Integer>();
    transactionInputsPlusOutputs    = new ArrayList<Integer>();
    finalTransactions               = new ArrayList<String>();
  }


  public static void main(String[] args) throws IllegalArgumentException {
    if(args.length != 3 || args == null) {
      throw new IllegalArgumentException("Invalid arguments");
    }

    init();

    BigInteger difficulty   = new BigInteger(args[1]);
    String prevHash         = args[2];
    BigInteger target       = Constants.MAX_DIFFICULTY.divide(difficulty);
    File transactionFile    = new File(args[0]);
    Scanner fileScan        = null;
    String nonce            = "    ";

    try {
      fileScan = new Scanner(transactionFile);
    } catch(FileNotFoundException e) {
      e.printStackTrace();
      System.exit(0);
    }

    while(fileScan.hasNext()) {
      //Array to hold inputs in [0] and outputs in [1]
      String transactionLine = fileScan.nextLine();

      String[] inputsAndOutputs = transactionLine.split(";");
      String[] inputsArray = inputsAndOutputs[0].split(",");
      String[] outputsArray = inputsAndOutputs[1].split(",");

      int inputsForThisTX = inputsArray.length;
      int outputsForThisTX = outputsArray.length;

      numInputs += inputsArray.length;
      numOutputs += outputsArray.length;

      int totalInputValue = 0;
      int totalOutputValue = 0;

      for(int i=0; i<inputsArray.length; i++) {
        totalInputValue += Integer.parseInt(inputsArray[i].split(">")[1]);
      }

      for(int i=0; i<outputsArray.length; i++) {
        totalOutputValue += Integer.parseInt(outputsArray[i].split(">")[1]);
      }

      int transactionFee = (totalInputValue - totalOutputValue);

      if(transactionFee > 0) {
        int txWeight = transactionFee/(inputsForThisTX + outputsForThisTX);
        transactionsInTransactionOrder.add(transactionLine);
        weightedTransactionFees.add(txWeight);
        transactionInputsPlusOutputs.add(inputsForThisTX+outputsForThisTX);
        totalTransactionFees += transactionFee;
        numTransactions++;
      }
    }

    //my attempt to optimize
    //Add transactions based on transaction weight
    int counter = 0;
    int totalNumTrans = numTransactions;
    if(numTransactions > 16) {
      totalNumTrans = 16;
    }
    while(counter < totalNumTrans) {
      int max = 0;
      int ind = 0;
      for(int i = 0; i < numTransactions; i++) {
        if(weightedTransactionFees.get(i) > max && weightedTransactionFees.get(i) != -1) {
          max = weightedTransactionFees.get(i);
          ind = i;
        }
      }
      if(transactionInputsPlusOutputs.get(ind) > 16) {
        weightedTransactionFees.add(ind, -1);
      } else {
        finalTransactions.add(transactionsInTransactionOrder.get(ind));
        transactionInputsPlusOutputs.add(ind, -1);
        weightedTransactionFees.add(ind, -1);
        counter++;
      }
    }

    addCoinbase();

    String concatRoot = calculateConcatRoot();
    long timeMS = System.currentTimeMillis();
    BigInteger blockHash = calculateBlockHash(prevHash, numInputs+numOutputs, timeMS, nonce, difficulty, concatRoot);

    while (!(blockHash.compareTo(target) == -1)) {
      nonce = incrementNonce(nonce);
      timeMS = System.currentTimeMillis();
      blockHash = calculateBlockHash(prevHash, numInputs+numOutputs, timeMS, nonce, difficulty, concatRoot);
    }

    System.out.println("CANDIDATE BLOCK = Hash " + blockHash.toString() + "\n---");
    System.out.println(prevHash);
    System.out.println(numInputs + numOutputs);
    System.out.println(timeMS);
    System.out.println(difficulty.toString());
    System.out.println(nonce);
    System.out.println(concatRoot);
    printTransactions();
  }

  private static void addCoinbase() {
    String coinbase = "1333dGpHU6gQShR596zbKHXEeSihdtoyLb>50";
    int coinbaseValue = 50 + totalTransactionFees;

    finalTransactions.add(coinbase);
    numTransactions++;
    numOutputs++;
  }

  private static String calculateConcatRoot() {
    String stringToHash = "";
    for(int i=0; i < finalTransactions.size(); i++) {
      stringToHash +=finalTransactions.get(i).trim();
    }
    String concatHash = Sha256Hash.calculateHash(stringToHash);
    return concatHash;
  }

  private static BigInteger calculateBlockHash(String prevHash, int inputsPlusOutputs, long timeMS, String nonce, BigInteger difficulty, String concatRoot) {
    String stringToHash = prevHash + inputsPlusOutputs + timeMS + difficulty + nonce + concatRoot;
    return Sha256Hash.hashBigInteger(stringToHash);
  }

  private static String incrementNonce(String oldNonce) {
    char[] nonceArray = oldNonce.toCharArray();
    if((int)nonceArray[0] < 126) {
      nonceArray[0]++;
    } else if((int)nonceArray[0] >= 126 && (int)nonceArray[1] < 126) {
      nonceArray[1]++;
    } else if((int)nonceArray[0] >= 126 && (int)nonceArray[1] >= 126 && (int)nonceArray[2] < 126) {
      nonceArray[2]++;
    } else if((int)nonceArray[0] >= 126 && (int)nonceArray[1] >= 126 && (int)nonceArray[2] >= 126 && (int)nonceArray[3] < 126) {
      nonceArray[3]++;
    } else {
      return "    ";
    }
    /*BigInteger bi = new BigInteger(oldNonce, 16);
    bi = bi.add(BigInteger.ONE);
    String newNonce = bi.toString(16);*/
    String newNonce = new String(nonceArray);
    return newNonce;
  }

  private static void printTransactions() {
    for(int i=0; i<finalTransactions.size(); i++) {
      System.out.println(finalTransactions.get(i));
    }
  }
}
