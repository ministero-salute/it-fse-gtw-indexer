package it.finanze.sanita.fse2.ms.gtwindexer.repository.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Model to save ini and eds invocation info.
 */
@Document(collection = "ini_eds_invocation")
@Data
@NoArgsConstructor
public class IniEdsInvocationETY {

	@Id
	private String id;
	
	@Field(name = "transaction_id")
	private String transactionId;
	
	@Field(name = "data")
	private org.bson.Document data;
	
	@Field(name = "metadata")
	private List<org.bson.Document> metadata;
	
	 
}