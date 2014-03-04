package tcode;

import log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/26/14
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class Register {
    public List<String> symids = new ArrayList<String>();
    public int registerNumber;

    public Register(int i) {
        registerNumber = i;
    }
}
