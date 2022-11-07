/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IniDeleteRequestDTO {
    private String idDoc;
    private String uuid;
    private String sub;
    private String iss;
    private String subject_organization;
    private String subject_organization_id;
    private String locality;
    private String subject_role;
    private String person_id;
    private String purpose_of_use;
    private String action_id;
    private String resource_hl7_type;
    private Boolean patient_consent;
}
