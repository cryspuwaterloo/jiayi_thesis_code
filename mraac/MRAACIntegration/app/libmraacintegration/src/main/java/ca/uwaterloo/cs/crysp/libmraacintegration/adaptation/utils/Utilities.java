package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.utils;

public class Utilities {
    public static String join(String deliminator, Iterable<String> strings) {
        StringBuilder result = new StringBuilder();
        boolean flag = false;
        for(String i: strings) {
            if (flag) {
                result.append(deliminator);
            }
            flag = true;
            result.append(i);

        }
        return result.toString();
    }


    public static String join(String deliminator, String... strings) {
        StringBuilder result = new StringBuilder();
        boolean flag = false;
        for(String i: strings) {
            if (flag) {
                result.append(deliminator);
            }
            flag = true;
            result.append(i);

        }
        return result.toString();
    }

}
