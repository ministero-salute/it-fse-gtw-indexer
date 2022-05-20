package it.finanze.sanita.fse2.ms.gtwindexer.repository.mongo;

import java.io.Serializable;

import it.finanze.sanita.fse2.ms.gtwindexer.repository.entity.IniEdsInvocationETY;

public interface IIniInvocationRepo extends Serializable {

	IniEdsInvocationETY findByTransactionId(String transactionID);
}
