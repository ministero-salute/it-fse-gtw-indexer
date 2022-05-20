package it.finanze.sanita.fse2.ms.gtwindexer.utility;

import org.jasypt.util.text.AES256TextEncryptor;

public class EncryptDecryptUtility {

    /** 
     *  Metodo per l'encrypt.
     *  @param pwd
     *  @param msg
     *  @return String
     */
	public static final String encrypt(String pwd, String msg) {
	    AES256TextEncryptor textEncryptor = new AES256TextEncryptor();
    	 
    	textEncryptor.setPassword(pwd);
    	return textEncryptor.encrypt(msg);
	}

	/** 
     *  Metodo per il decrypt.
     *  @param pwd
     *  @param cryptedMsg
     *  @return String
     */
	public static final String decrypt(String pwd, String cryptedMsg) {
    	AES256TextEncryptor textEncryptor = new AES256TextEncryptor();
    	textEncryptor.setPassword(pwd);
    	return textEncryptor.decrypt(cryptedMsg);
	}


     /** 
     *  Encrypt method.
     *  @param pwd
     *  @param obj
     *  @return String
     */
	public static final String encryptObject(String pwd, Object obj) {
		String json = StringUtility.toJSON(obj);
		return encrypt(pwd, json);
	}

	 /** 
     *  Decrypt method.
     *  @param pwd
     *  @param obj
     *  @return String
     */
	public static final <T> T decryptObject(String pwd, String cryptedMsg, Class<T> cls) {
		String json = decrypt(pwd, cryptedMsg);
		return StringUtility.fromJSON(json, cls);
	}
    
}
