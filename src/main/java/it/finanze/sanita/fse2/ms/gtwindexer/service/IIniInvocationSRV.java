package it.finanze.sanita.fse2.ms.gtwindexer.service;

import java.io.Serializable;

public interface IIniInvocationSRV extends Serializable {

	Boolean findAndSendToIniByTransactionId(String transactionId);
}
