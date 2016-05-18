import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;

public class TruthTable {
  private ArrayList rules = null;
  private Var[] vars = null;
  private HashMap letterToVal = null;

  private boolean isFile = false;
  private static PrintStream out = System.out;

  public static final String VERSION = "1.2.2";
  
  public static void main(String[] args) {
    TruthTable table = new TruthTable();
    BufferedReader in = null;

    System.out.println("TruthTable version: "+VERSION);
    System.out.println("\nHello and welcome to Greg's Truth Table generator!"
               +"\nVery little error checking has been built into this so"
               +"\ndo try to follow these simple conventions:\n"
               +"\n-----------------------------------------------------\n"
               +"AND: ^ OR: v NOT: ~"
               +"\nImplication: > Biconditional: ="
               +"\n\nThe following letters are not allowed as variables:"
               +"\nx v i d"
               +"\nYou may use the uppercase versions"
               +"\n-----------------------------------------------------\n");
    
    String prop = null, temp = null;
    boolean done = false;
    int i = 0;

    try {
      in = new BufferedReader(new InputStreamReader(System.in));
      
      if (args.length == 2 && args[0].equals("-f"))
          table.setFile(args[1]);
      
      while (!done) {
        System.out.print("Enter the single-character variables you will use"+
                 "\nseparated by spaces or press return to quit:\n> ");

        temp = in.readLine();

        if (temp.length() == 0)
          break;
        
        try {
          table.addVars(temp.split(" "));
        } catch (IllegalArgumentException e) {
          System.out.println(e.getMessage());
          continue;
        }

        System.out.println("\nNow enter your propositions one at a time\n"
                   +"Press the return key to print the table:");
        System.out.print("> ");
        while (!(prop = in.readLine().trim()).equals("")) {
          System.out.print("> ");
          table.addRule(prop);
        }
        
        table.print();
        
        if (table.fileOpen()) 
          System.out.println("\nTable written to: "+args[1]+"\n");
        
        table.reset();
      }
      table.setFile(null);  // close the file
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public TruthTable() {
    rules = new ArrayList();
    letterToVal = new HashMap();
  }
  
  public TruthTable(int len) {
    vars = new Var[len];
    rules = new ArrayList();
    letterToVal = new HashMap();
  }
  
  public void addVars(String[] variables) {
    if (vars == null)
      vars = new Var[variables.length];
    
    for (int i=0; i<variables.length; i++) {
      String var = variables[i];
      if ( var.indexOf('x') != -1 || var.indexOf('v') != -1
         || var.indexOf('i') != -1 || var.indexOf('d') != -1 ) {

        vars = null;
        throw new IllegalArgumentException("Bad variable name");
      }
      
      letterToVal.put(var, new Integer(i));
      vars[i]=new Var(var);
    }
  }

  public void reset() {
    letterToVal.clear();
    rules.clear();
    vars = null;
  }
  
  public void addRule(String rule) {
    rules.add(rule);
  }

  public void setFile(String file) {
    if (file != null) {
      isFile = true;
      try {
        out = new PrintStream(new FileOutputStream(file));
      } catch (IOException e) {
        e.printStackTrace();
        out.close();
        out = System.out;
        isFile = false;
      }
    } else {
      if (isFile) {
        isFile = false;
        out.close();
      }
    }
  }

  public boolean fileOpen() {
    return isFile;
  }
  
  public void finalize() {
    if (isFile) {
      isFile = false;
      out.close();
    }
  }
  
  public static void print(String what) {
    out.print(what);
  }

  public static void println(String what) {
    print(what+"\n");
  }
  
  public void print() {
    int i=0, j=0, k=0, len1 = rules.size(), len2 = vars.length;
    boolean[] vals = new boolean[len2];
    char c;
    int accume = 0;
    
    accume += 2*len2;
    for (i=0; i<len1; i++)
      accume += 3 + ((String)rules.get(i)).length();

    char[] dashes = new char[accume+2];
    
    for (i=0; i<=accume; i++)
      dashes[i]='-';
    dashes[i]='\n';
    
    out.print('\n');
    out.print(dashes);

    out.print(' ');
    for (i=0; i<len2; i++) {
      print( vars[i].letter + " ");
      vals[i]=true;
    }
    for (i=0; i<len1; i++)
      print("| "+rules.get(i)+" ");

    out.print('\n');
    out.print(dashes);
        
    int num = (int)Math.pow(2, len2);
    String rule = null;
    int ruleLength = 0, nRuleLen;
    
    ArrayList modifiedRule = new ArrayList();
    String[] pieces = null;
    
    for (i=0; i<num; i++) {
      // for every truth value
      initVals(vals, i);
      out.print(' ');
      printTruths(vals);
      
      for (j=0; j<len1; j++) {
        // for every rule
        rule = (String)rules.get(j);
        ruleLength = rule.length();
        
        rule = rule.replaceAll("=", "d");
        rule = rule.replaceAll(">", "i");
        
        for (k=0; k<len2; k++) {
          // replace vars with vals
          rule = rule.replaceAll(vars[k].letter,
                       boolToStr(vals[((Integer)letterToVal.
                               get(vars[k].letter)).intValue()]));
        }

        nRuleLen = rule.length();
        k=0;
        
        while (k<nRuleLen) {
          c = rule.charAt(k);
          if (c != ' ')
            modifiedRule.add(String.valueOf(c));
          k++;
        }
          
        out.print('|');
        printSpaces(ruleLength+2, true);
        print(parse(modifiedRule, 0, false));
        printSpaces(ruleLength+2, false);
        
        modifiedRule.clear();
      }
      out.print('\n');
    }
    out.print(dashes);
    out.print('\n');
  }

  private void printSpaces(int fullsize, boolean first) {
    int len;
    
    if (fullsize%2==0 && !first)
      len = fullsize/2 - 1;
    else
      len = fullsize/2;
    
    for (int i=0; i<len; i++)
      out.print(' ');
  }
  
  private String parse(ArrayList prop, int index, boolean comeback) {
    // do operators in order of precedence
    
    parseSubGroup(prop, index, "~", comeback);
    parseSubGroup(prop, index, "^", comeback);
    parseSubGroup(prop, index, "v", comeback);
    parseSubGroup(prop, index, "x", comeback);
    parseSubGroup(prop, index, "i", comeback);
    parseSubGroup(prop, index, "d", comeback);
    
    return (String)prop.get(0);
  }
  
  private void parseSubGroup(ArrayList prop, int i, String delim, boolean comeback) {
    String temp = null;
    
    // has to be prop.size() because that value will change
    while (i < prop.size()) {
      
      temp = (String)prop.get(i);
      
      if (temp.equals("(")) {
        prop.remove(i);
        parse(prop, i, true);
        
        if (i+1 < prop.size())
          prop.remove(i+1);
      } else if (temp.equals(")")) {
        if (comeback)
          return; // can't delete ')' or it won't return

        // i don't think this will ever be called... but ...
        // yeah, too scared to remove it cause it works :-)
        prop.remove(i); // remove side effect from previous operation
        
        continue;
      }
      
      if (temp.equals(delim)) {
        
        if (temp.equals("~")) {
          prop.remove(i); // remove '!' to get T/F or (
          temp = (String)prop.get(i);
        
          if (!temp.equals("(")) {
          
            if (temp.equals("T"))
              prop.set(i, "F");
            else
              prop.set(i, "T");
            
          } else {
            parse(prop, i+1, true);
            // we're left with (T/F)
            prop.remove(i); // remove the '('
            
            temp = (String)prop.get(i);
            if (temp.equals("T"))
              prop.set(i, "F");
            else
              prop.set(i, "T");
            
            if (i+1 < prop.size())
              prop.remove(i+1); // remove ')'
          }
          
        } else {
          // we are an operator that takes two operands
          String prev = (String)prop.get(i-1);
          String next = (String)prop.get(i+1);
          
          if (!next.equals("(")) {

            if (process(prev, next, temp))
              prop.set(i-1, "T");
            else
              prop.set(i-1, "F");
            
            prop.remove(i); // remove the operator
            prop.remove(i); // remove the next truth value
            i--; // place ourselves on the resultant truth value
          } else {
            parse(prop, i+2, true);
            prop.remove(i+1); // remove the '('
            
            next = (String)prop.get(i+1); // get the T/F
            
            if (process(prev, next, temp))
              prop.set(i-1, "T");
            else
              prop.set(i-1, "F");
            
            prop.remove(i); // remove op
            prop.remove(i); // remove next truth value
            if (i < prop.size())
              prop.remove(i); // remove matching ')'
            i--;
          }
        }
      }
      i++;
    }
  }
  
  private boolean process(String pStr, String qStr, String opStr) {
    boolean p = (pStr.equals("T")) ? true : false;
    boolean q = (qStr.equals("T")) ? true : false;
    char op = opStr.charAt(0);
    
    switch (op) {
      case '^':
        return p && q;
      case 'v':
        return p || q;
      case 'x':
        return p != q;
      case 'i':
        return !p || q;
      case 'd':
        return p == q;
      default:
        throw new RuntimeException("unknown operator: "+op);
    }
  }
  
  private String boolToStr(boolean val) {
    return (val) ? "T" : "F";
  }
  
  private void printTruths(boolean[] vals) {
    for (int i=0; i<vals.length; i++) {
      if (vals[i])
        print("T ");
      else
        print("F ");
    }
  }
  
  private void initVals(boolean[] vals, int num) {
    int counter = 1;
    for (int i=1; i<=vals.length; i++) {
      if ( (num & counter) > 0 )
        vals[vals.length-i] = false;
      else
        vals[vals.length-i] = true;
      
      counter <<= 1;
    }
  }
  private class Var {
    public String letter;
    public boolean value;
    public Var(String var) {
      letter = var;
    }
  }
}