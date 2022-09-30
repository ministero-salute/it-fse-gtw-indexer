package it.finanze.sanita.fse2.ms.gtwindexer.dto.request;


import it.finanze.sanita.fse2.ms.gtwindexer.enums.ProcessorOperationEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndexerValueDTO {
    /**
     * Workflow instance id.
     */
    private String workflowInstanceId;

    /**
     * Identifier of document to be updated. Is not {@code null} only if the notification is sent for update.
     */
    private String idDoc;

    /**
     * Enum of the operation to perform on EDS
     */
    private ProcessorOperationEnum edsDPOperation;
}
