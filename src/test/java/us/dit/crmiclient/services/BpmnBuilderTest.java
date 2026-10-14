package us.dit.crmiclient.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BpmnBuilderTest {

    @Autowired
    private BpmnBuilder bpmnBuilder;

    @Test
    void testPlanDefinitionToBpmn() {
        String rawPlanDefinition = "{\n" + //
                        "  \"resourceType\": \"PlanDefinition\",\n" + //
                        "  \"id\": \"low-suicide-risk-order-set\",\n" + //
                        "  \"text\": {\n" + //
                        "    \"status\": \"generated\",\n" + //
                        "    \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Id: </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">PlanDefinition/low-suicide-risk-order-set</td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Identifier: </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">\\n                  <b>value: </b>\\n                  <span>mmi:low-suicide-risk-order-set</span>\\n               </td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Title: </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">Low Suicide Risk Order Set</td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Status: </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">draft</td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Description: </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">Orders to be applied to a patient characterized as low suicide risk.</td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Purpose: </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">This order set helps ensure consistent application of appropriate orders for the care of low suicide risk patients.</td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Usage: </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">This order set should be applied after assessing a patient for suicide risk, when the findings of that assessment indicate the patient has low suicide risk.</td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Context: </b>\\n               </td>\\n            </tr>\\n            <tr style=\\\"vertical-align: top;\\\">\\n               <td style=\\\"padding-right: 25px;\\\">\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>system: </b>\\n                     <span>http://terminology.hl7.org/CodeSystem/usage-context-type</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>code: </b>\\n                     <span>age</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>value: </b>\\n                     <br/>\\n                     <span>\\n                        <span>\\n                           <span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>system: </b>\\n                                 <span>https://meshb.nlm.nih.gov</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>code: </b>\\n                                 <span>D000328</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>display: </b>\\n                                 <span>Adult</span>\\n                              </span>\\n                           </span>\\n                        </span>\\n                     </span>\\n                  </p>\\n               </td>\\n            </tr>\\n         </table>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Context: </b>\\n               </td>\\n            </tr>\\n            <tr style=\\\"vertical-align: top;\\\">\\n               <td style=\\\"padding-right: 25px;\\\">\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>system: </b>\\n                     <span>http://terminology.hl7.org/CodeSystem/usage-context-type</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>code: </b>\\n                     <span>focus</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>value: </b>\\n                     <br/>\\n                     <span>\\n                        <span>\\n                           <span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>system: </b>\\n                                 <span>http://snomed.info/sct</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>code: </b>\\n                                 <span>87512008</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>display: </b>\\n                                 <span>Mild major depression</span>\\n                              </span>\\n                           </span>\\n                        </span>\\n                     </span>\\n                  </p>\\n               </td>\\n            </tr>\\n         </table>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Context: </b>\\n               </td>\\n            </tr>\\n            <tr style=\\\"vertical-align: top;\\\">\\n               <td style=\\\"padding-right: 25px;\\\">\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>system: </b>\\n                     <span>http://terminology.hl7.org/CodeSystem/usage-context-type</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>code: </b>\\n                     <span>focus</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>value: </b>\\n                     <br/>\\n                     <span>\\n                        <span>\\n                           <span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>system: </b>\\n                                 <span>http://snomed.info/sct</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>code: </b>\\n                                 <span>40379007</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>display: </b>\\n                                 <span>Major depression, recurrent, mild</span>\\n                              </span>\\n                           </span>\\n                        </span>\\n                     </span>\\n                  </p>\\n               </td>\\n            </tr>\\n         </table>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Context: </b>\\n               </td>\\n            </tr>\\n            <tr style=\\\"vertical-align: top;\\\">\\n               <td style=\\\"padding-right: 25px;\\\">\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>system: </b>\\n                     <span>http://terminology.hl7.org/CodeSystem/usage-context-type</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>code: </b>\\n                     <span>focus</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>value: </b>\\n                     <br/>\\n                     <span>\\n                        <span>\\n                           <span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>system: </b>\\n                                 <span>http://snomed.info/sct</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>code: </b>\\n                                 <span>394687007</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>display: </b>\\n                                 <span>Low suicide risk</span>\\n                              </span>\\n                           </span>\\n                        </span>\\n                     </span>\\n                  </p>\\n               </td>\\n            </tr>\\n         </table>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Context: </b>\\n               </td>\\n            </tr>\\n            <tr style=\\\"vertical-align: top;\\\">\\n               <td style=\\\"padding-right: 25px;\\\">\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>system: </b>\\n                     <span>http://terminology.hl7.org/CodeSystem/usage-context-type</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>code: </b>\\n                     <span>focus</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>value: </b>\\n                     <br/>\\n                     <span>\\n                        <span>\\n                           <span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>system: </b>\\n                                 <span>http://snomed.info/sct</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>code: </b>\\n                                 <span>225337009</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>display: </b>\\n                                 <span>Suicide risk assessment</span>\\n                              </span>\\n                           </span>\\n                        </span>\\n                     </span>\\n                  </p>\\n               </td>\\n            </tr>\\n         </table>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Context: </b>\\n               </td>\\n            </tr>\\n            <tr style=\\\"vertical-align: top;\\\">\\n               <td style=\\\"padding-right: 25px;\\\">\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>system: </b>\\n                     <span>http://terminology.hl7.org/CodeSystem/usage-context-type</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>code: </b>\\n                     <span>user</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>value: </b>\\n                     <br/>\\n                     <span>\\n                        <span>\\n                           <span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>system: </b>\\n                                 <span>http://snomed.info/sct</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>code: </b>\\n                                 <span>309343006</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>display: </b>\\n                                 <span>Physician</span>\\n                              </span>\\n                           </span>\\n                        </span>\\n                     </span>\\n                  </p>\\n               </td>\\n            </tr>\\n         </table>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Context: </b>\\n               </td>\\n            </tr>\\n            <tr style=\\\"vertical-align: top;\\\">\\n               <td style=\\\"padding-right: 25px;\\\">\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>system: </b>\\n                     <span>http://terminology.hl7.org/CodeSystem/usage-context-type</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>code: </b>\\n                     <span>venue</span>\\n                  </p>\\n                  <p style=\\\"padding-left: 25px; margin-top: 5px; margin-bottom: 5px;\\\">\\n                     <b>value: </b>\\n                     <br/>\\n                     <span>\\n                        <span>\\n                           <span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>system: </b>\\n                                 <span>http://snomed.info/sct</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>code: </b>\\n                                 <span>440655000</span>\\n                                 <br/>\\n                              </span>\\n                              <span style=\\\"padding-left: 25px;\\\">\\n                                 <b>display: </b>\\n                                 <span>Outpatient environment</span>\\n                              </span>\\n                           </span>\\n                        </span>\\n                     </span>\\n                  </p>\\n               </td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Topic: </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">\\n                  <span>\\n                     <b>text: </b>\\n                     <span>Suicide risk assessment</span>\\n                  </span>\\n               </td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Contributor: </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">\\n                  <b>\\n                     <span>author</span>:\\n                  </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 50px; padding-right: 25px;\\\">Motive Medical Intelligence</td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <table class=\\\"grid dict\\\">\\n            <tr>\\n               <td>\\n                  <b>Library: </b>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">\\n                  <b>reference: </b>\\n                  <span>Library/suiciderisk-orderset-logic</span>\\n               </td>\\n            </tr>\\n            <tr>\\n               <td style=\\\"padding-left: 25px; padding-right: 25px;\\\">\\n                  <b>display: </b>\\n                  <span>SuicideRiskLogic</span>\\n               </td>\\n            </tr>\\n         </table>\\n         <p/>\\n         <h2>Actions</h2>\\n         <p style=\\\"width: 100%;\\\" class=\\\"hierarchy\\\">\\n            <span>\\n               <b>Step: </b>\\n               <br/>\\n               <span style=\\\"padding-left: 25px;\\\">\\n                  <b>title: </b>\\n                  <span>Suicide Risk Assessment and Outpatient Management</span>\\n                  <br/>\\n               </span>\\n               <span style=\\\"padding-left: 25px;\\\">\\n                  <span>\\n                     <b>Step: </b>\\n                     <br/>\\n                     <span style=\\\"padding-left: 50px;\\\">\\n                        <b>title: </b>\\n                        <span>Consults and Referrals</span>\\n                        <br/>\\n                     </span>\\n                     <span style=\\\"padding-left: 50px;\\\">\\n                        <span>\\n                           <b>Step: </b>\\n                           <br/>\\n                           <span style=\\\"padding-left: 75px;\\\">\\n                              <b>text: </b>\\n                              <span>Refer to outpatient mental health program for evaluation and treatment of mental health conditions now</span>\\n                              <br/>\\n                           </span>\\n                           <span style=\\\"padding-left: 75px;\\\">\\n                              <b>condition: </b>\\n                              <br/>\\n                              <span style=\\\"padding-left: 100px;\\\">\\n                                 <b>reference: </b>\\n                                 <br/>\\n                                 <span style=\\\"padding-left: 125px;\\\">#referralToMentalHealthCare</span>\\n                                 <span>\\n                                    <span style=\\\"padding-left: 75px;\\\">\\n                                       <br/>\\n                                       <span style=\\\"padding-left: 100px;\\\">\\n                                          <b>description: </b>\\n                                          <span>refer to primary care mental-health integrated care program for evaluation and treatment of mental health conditions now</span>\\n                                          <br/>\\n                                       </span>\\n                                       <span style=\\\"padding-left: 100px;\\\">\\n                                          <b>category: </b>\\n                                          <span>referral</span>\\n                                          <br/>\\n                                       </span>\\n                                    </span>\\n                                 </span>\\n                              </span>\\n                           </span>\\n                           <span style=\\\"padding-left: 50px;\\\"/>\\n                        </span>\\n                     </span>\\n                  </span>\\n                  <span>\\n                     <b>Step: </b>\\n                     <br/>\\n                     <span style=\\\"padding-left: 50px;\\\">\\n                        <b>title: </b>\\n                        <span>Medications</span>\\n                        <br/>\\n                     </span>\\n                     <span style=\\\"padding-left: 50px;\\\">\\n                        <span>\\n                           <b>Step: </b>\\n                           <br/>\\n                           <span style=\\\"padding-left: 75px;\\\">\\n                              <b>title: </b>\\n                              <span>First-Line Antidepressants</span>\\n                              <br/>\\n                           </span>\\n                           <span style=\\\"padding-left: 75px;\\\">\\n                              <span>\\n                                 <b>Step: </b>\\n                                 <br/>\\n                                 <span style=\\\"padding-left: 100px;\\\">\\n                                    <b>title: </b>\\n                                    <span>Selective Serotonin Reuptake Inhibitors (Choose a mazimum of one or document reasons for exception)</span>\\n                                    <br/>\\n                                 </span>\\n                                 <span style=\\\"padding-left: 100px;\\\">\\n                                    <span>\\n                                       <b>Step: </b>\\n                                       <br/>\\n                                       <span style=\\\"padding-left: 125px;\\\">\\n                                          <b>text: </b>\\n                                          <span>citalopram 20 mg tablet 1 tablet oral 1 time daily now (30 table; 3 refills)</span>\\n                                          <br/>\\n                                       </span>\\n                                       <span style=\\\"padding-left: 125px;\\\">\\n                                          <b>condition: </b>\\n                                          <br/>\\n                                          <span style=\\\"padding-left: 150px;\\\">\\n                                             <b>reference: </b>\\n                                             <br/>\\n                                             <span style=\\\"padding-left: 175px;\\\">#citalopramPrescription</span>\\n                                             <span/>\\n                                             <span>\\n                                                <span style=\\\"padding-left: 125px;\\\">\\n                                                   <br/>\\n                                                   <span style=\\\"padding-left: 150px;\\\">\\n                                                      <b>category: </b>\\n                                                      <span>drug</span>\\n                                                      <br/>\\n                                                   </span>\\n                                                </span>\\n                                             </span>\\n                                             <span/>\\n                                             <span/>\\n                                          </span>\\n                                       </span>\\n                                       <span style=\\\"padding-left: 100px;\\\"/>\\n                                    </span>\\n                                    <span>\\n                                       <b>Step: </b>\\n                                       <br/>\\n                                       <span style=\\\"padding-left: 125px;\\\">\\n                                          <b>text: </b>\\n                                          <span>escitalopram 10 mg tablet 1 tablet oral 1 time daily now (30 tablet; 3 refills)</span>\\n                                          <br/>\\n                                       </span>\\n                                       <span style=\\\"padding-left: 100px;\\\"/>\\n                                    </span>\\n                                    <span>\\n                                       <b>Step: </b>\\n                                       <br/>\\n                                       <span style=\\\"padding-left: 125px;\\\">\\n                                          <b>text: </b>\\n                                          <span>fluoxetine 20 mg capsule 1 capsule oral 1 time daily now (30 tablet; 3 refills)</span>\\n                                          <br/>\\n                                       </span>\\n                                       <span style=\\\"padding-left: 100px;\\\"/>\\n                                    </span>\\n                                    <span>\\n                                       <b>Step: </b>\\n                                       <br/>\\n                                       <span style=\\\"padding-left: 125px;\\\">\\n                                          <b>text: </b>\\n                                          <span>paroxetine 20 mg tablet 1 tablet oral 1 time daily now (30 tablet; 3 refills)</span>\\n                                          <br/>\\n                                       </span>\\n                                       <span style=\\\"padding-left: 100px;\\\"/>\\n                                    </span>\\n                                    <span>\\n                                       <b>Step: </b>\\n                                       <br/>\\n                                       <span style=\\\"padding-left: 125px;\\\">\\n                                          <b>text: </b>\\n                                          <span>sertraline 50 mg tablet 1 tablet oral 1 time daily now (30 tablet; 3 refills)</span>\\n                                          <br/>\\n                                       </span>\\n                                       <span style=\\\"padding-left: 100px;\\\"/>\\n                                    </span>\\n                                 </span>\\n                              </span>\\n                              <span>\\n                                 <b>Step: </b>\\n                                 <br/>\\n                                 <span style=\\\"padding-left: 125px;\\\">\\n                                    <b>text: </b>\\n                                    <span>Dopamine Norepinephrine Reuptake Inhibitors (Choose a maximum of one or document reasons for exception)</span>\\n                                    <br/>\\n                                 </span>\\n                                 <span style=\\\"padding-left: 75px;\\\"/>\\n                              </span>\\n                              <span>\\n                                 <b>Step: </b>\\n                                 <br/>\\n                                 <span style=\\\"padding-left: 100px;\\\">\\n                                    <b>text: </b>\\n                                    <span>Serotonin Norepinephrine Reuptake Inhibitors (Choose a maximum of one or doument reasons for exception)</span>\\n                                    <br/>\\n                                 </span>\\n                                 <span style=\\\"padding-left: 75px;\\\"/>\\n                              </span>\\n                              <span>\\n                                 <b>Step: </b>\\n                                 <br/>\\n                                 <span style=\\\"padding-left: 100px;\\\">\\n                                    <b>text: </b>\\n                                    <span>Norepinephrine-Serotonin Modulators (Choose a maximum of one or document reasons for exception)</span>\\n                                    <br/>\\n                                 </span>\\n                                 <span style=\\\"padding-left: 100px;\\\"/>\\n                              </span>\\n                           </span>\\n                        </span>\\n                     </span>\\n                  </span>\\n               </span>\\n            </span>\\n         </p>\\n      </div>\"\n" + //
                        "  },\n" + //
                        "  \"contained\": [\n" + //
                        "    {\n" + //
                        "      \"resourceType\": \"ActivityDefinition\",\n" + //
                        "      \"id\": \"referralToMentalHealthCare\",\n" + //
                        "      \"status\": \"draft\",\n" + //
                        "      \"description\": \"refer to primary care mental-health integrated care program for evaluation and treatment of mental health conditions now\",\n" + //
                        "      \"kind\": \"ServiceRequest\",\n" + //
                        "      \"code\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"http://snomed.info/sct\",\n" + //
                        "            \"code\": \"306206005\"\n" + //
                        "          }\n" + //
                        "        ],\n" + //
                        "        \"text\": \"Referral to service (procedure)\"\n" + //
                        "      },\n" + //
                        "      \"timingTiming\": {\n" + //
                        "        \"_event\": [\n" + //
                        "          {\n" + //
                        "            \"extension\": [\n" + //
                        "              {\n" + //
                        "                \"url\": \"http://hl7.org/fhir/StructureDefinition/cqf-expression\",\n" + //
                        "                \"valueExpression\": {\n" + //
                        "                  \"language\": \"text/cql\",\n" + //
                        "                  \"expression\": \"Now()\"\n" + //
                        "                }\n" + //
                        "              }\n" + //
                        "            ]\n" + //
                        "          }\n" + //
                        "        ]\n" + //
                        "      },\n" + //
                        "      \"participant\": [\n" + //
                        "        {\n" + //
                        "          \"type\": \"practitioner\"\n" + //
                        "        }\n" + //
                        "      ]\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"resourceType\": \"ActivityDefinition\",\n" + //
                        "      \"id\": \"citalopramPrescription\",\n" + //
                        "      \"status\": \"draft\",\n" + //
                        "      \"kind\": \"MedicationRequest\",\n" + //
                        "      \"productReference\": {\n" + //
                        "        \"reference\": \"#citalopramMedication\"\n" + //
                        "      },\n" + //
                        "      \"dosage\": [\n" + //
                        "        {\n" + //
                        "          \"text\": \"1 tablet oral 1 time daily\",\n" + //
                        "          \"timing\": {\n" + //
                        "            \"repeat\": {\n" + //
                        "              \"frequency\": 1,\n" + //
                        "              \"period\": 1,\n" + //
                        "              \"periodUnit\": \"d\"\n" + //
                        "            }\n" + //
                        "          },\n" + //
                        "          \"route\": {\n" + //
                        "            \"coding\": [\n" + //
                        "              {\n" + //
                        "                \"code\": \"26643006\",\n" + //
                        "                \"display\": \"Oral route (qualifier value)\"\n" + //
                        "              }\n" + //
                        "            ],\n" + //
                        "            \"text\": \"Oral route (qualifier value)\"\n" + //
                        "          },\n" + //
                        "          \"doseAndRate\": [\n" + //
                        "            {\n" + //
                        "              \"type\": {\n" + //
                        "                \"coding\": [\n" + //
                        "                  {\n" + //
                        "                    \"system\": \"http://terminology.hl7.org/CodeSystem/dose-rate-type\",\n" + //
                        "                    \"code\": \"ordered\",\n" + //
                        "                    \"display\": \"Ordered\"\n" + //
                        "                  }\n" + //
                        "                ]\n" + //
                        "              },\n" + //
                        "              \"doseQuantity\": {\n" + //
                        "                \"value\": 1,\n" + //
                        "                \"unit\": \"{tbl}\"\n" + //
                        "              }\n" + //
                        "            }\n" + //
                        "          ]\n" + //
                        "        }\n" + //
                        "      ],\n" + //
                        "      \"dynamicValue\": [\n" + //
                        "        {\n" + //
                        "          \"path\": \"dispenseRequest.numberOfRepeatsAllowed\",\n" + //
                        "          \"expression\": {\n" + //
                        "            \"language\": \"text/cql\",\n" + //
                        "            \"expression\": \"3\"\n" + //
                        "          }\n" + //
                        "        },\n" + //
                        "        {\n" + //
                        "          \"path\": \"dispenseRequest.quantity\",\n" + //
                        "          \"expression\": {\n" + //
                        "            \"language\": \"text/cql\",\n" + //
                        "            \"expression\": \"30 '{tbl}'\"\n" + //
                        "          }\n" + //
                        "        }\n" + //
                        "      ]\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"resourceType\": \"Medication\",\n" + //
                        "      \"id\": \"citalopramMedication\",\n" + //
                        "      \"code\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"http://www.nlm.nih.gov/research/umls/rxnorm\",\n" + //
                        "            \"code\": \"200371\"\n" + //
                        "          }\n" + //
                        "        ],\n" + //
                        "        \"text\": \"citalopram\"\n" + //
                        "      },\n" + //
                        "      \"form\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"http://snomed.info/sct\",\n" + //
                        "            \"code\": \"385055001\",\n" + //
                        "            \"display\": \"Tablet dose form\"\n" + //
                        "          }\n" + //
                        "        ],\n" + //
                        "        \"text\": \"Tablet dose form\"\n" + //
                        "      },\n" + //
                        "      \"ingredient\": [\n" + //
                        "        {\n" + //
                        "          \"itemReference\": {\n" + //
                        "            \"reference\": \"#citalopramSubstance\"\n" + //
                        "          },\n" + //
                        "          \"strength\": {\n" + //
                        "            \"numerator\": {\n" + //
                        "              \"value\": 20,\n" + //
                        "              \"unit\": \"mg\"\n" + //
                        "            },\n" + //
                        "            \"denominator\": {\n" + //
                        "              \"value\": 1,\n" + //
                        "              \"unit\": \"{tbl}\"\n" + //
                        "            }\n" + //
                        "          }\n" + //
                        "        }\n" + //
                        "      ]\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"resourceType\": \"Substance\",\n" + //
                        "      \"id\": \"citalopramSubstance\",\n" + //
                        "      \"code\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"http://www.nlm.nih.gov/research/umls/rxnorm\",\n" + //
                        "            \"code\": \"2556\"\n" + //
                        "          }\n" + //
                        "        ],\n" + //
                        "        \"text\": \"citalopram\"\n" + //
                        "      }\n" + //
                        "    }\n" + //
                        "  ],\n" + //
                        "  \"url\": \"http://motivemi.com/artifacts/PlanDefinition/low-suicide-risk-order-set\",\n" + //
                        "  \"identifier\": [\n" + //
                        "    {\n" + //
                        "      \"use\": \"official\",\n" + //
                        "      \"system\": \"http://motivemi.com/artifacts\",\n" + //
                        "      \"value\": \"mmi:low-suicide-risk-order-set\"\n" + //
                        "    }\n" + //
                        "  ],\n" + //
                        "  \"version\": \"1.0.0\",\n" + //
                        "  \"name\": \"LowSuicideRiskOrderSet\",\n" + //
                        "  \"title\": \"Low Suicide Risk Order Set\",\n" + //
                        "  \"status\": \"active\",\n" + //
                        "  \"experimental\": true,\n" + //
                        "  \"date\": \"2015-08-15\",\n" + //
                        "  \"publisher\": \"Motive Medical Intelligence\",\n" + //
                        "  \"contact\": [\n" + //
                        "    {\n" + //
                        "      \"telecom\": [\n" + //
                        "        {\n" + //
                        "          \"system\": \"phone\",\n" + //
                        "          \"value\": \"415-362-4007\",\n" + //
                        "          \"use\": \"work\"\n" + //
                        "        },\n" + //
                        "        {\n" + //
                        "          \"system\": \"email\",\n" + //
                        "          \"value\": \"info@motivemi.com\",\n" + //
                        "          \"use\": \"work\"\n" + //
                        "        }\n" + //
                        "      ]\n" + //
                        "    }\n" + //
                        "  ],\n" + //
                        "  \"description\": \"Orders to be applied to a patient characterized as low suicide risk.\",\n" + //
                        "  \"useContext\": [\n" + //
                        "    {\n" + //
                        "      \"code\": {\n" + //
                        "        \"system\": \"http://terminology.hl7.org/CodeSystem/usage-context-type\",\n" + //
                        "        \"code\": \"age\"\n" + //
                        "      },\n" + //
                        "      \"valueCodeableConcept\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"https://meshb.nlm.nih.gov\",\n" + //
                        "            \"code\": \"D000328\",\n" + //
                        "            \"display\": \"Adult\"\n" + //
                        "          }\n" + //
                        "        ]\n" + //
                        "      }\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"code\": {\n" + //
                        "        \"system\": \"http://terminology.hl7.org/CodeSystem/usage-context-type\",\n" + //
                        "        \"code\": \"focus\"\n" + //
                        "      },\n" + //
                        "      \"valueCodeableConcept\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"http://snomed.info/sct\",\n" + //
                        "            \"code\": \"87512008\",\n" + //
                        "            \"display\": \"Mild major depression\"\n" + //
                        "          }\n" + //
                        "        ]\n" + //
                        "      }\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"code\": {\n" + //
                        "        \"system\": \"http://terminology.hl7.org/CodeSystem/usage-context-type\",\n" + //
                        "        \"code\": \"focus\"\n" + //
                        "      },\n" + //
                        "      \"valueCodeableConcept\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"http://snomed.info/sct\",\n" + //
                        "            \"code\": \"40379007\",\n" + //
                        "            \"display\": \"Major depression, recurrent, mild\"\n" + //
                        "          }\n" + //
                        "        ]\n" + //
                        "      }\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"code\": {\n" + //
                        "        \"system\": \"http://terminology.hl7.org/CodeSystem/usage-context-type\",\n" + //
                        "        \"code\": \"focus\"\n" + //
                        "      },\n" + //
                        "      \"valueCodeableConcept\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"http://snomed.info/sct\",\n" + //
                        "            \"code\": \"394687007\",\n" + //
                        "            \"display\": \"Low suicide risk\"\n" + //
                        "          }\n" + //
                        "        ]\n" + //
                        "      }\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"code\": {\n" + //
                        "        \"system\": \"http://terminology.hl7.org/CodeSystem/usage-context-type\",\n" + //
                        "        \"code\": \"focus\"\n" + //
                        "      },\n" + //
                        "      \"valueCodeableConcept\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"http://snomed.info/sct\",\n" + //
                        "            \"code\": \"225337009\",\n" + //
                        "            \"display\": \"Suicide risk assessment\"\n" + //
                        "          }\n" + //
                        "        ]\n" + //
                        "      }\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"code\": {\n" + //
                        "        \"system\": \"http://terminology.hl7.org/CodeSystem/usage-context-type\",\n" + //
                        "        \"code\": \"user\"\n" + //
                        "      },\n" + //
                        "      \"valueCodeableConcept\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"http://snomed.info/sct\",\n" + //
                        "            \"code\": \"309343006\",\n" + //
                        "            \"display\": \"Physician\"\n" + //
                        "          }\n" + //
                        "        ]\n" + //
                        "      }\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"code\": {\n" + //
                        "        \"system\": \"http://terminology.hl7.org/CodeSystem/usage-context-type\",\n" + //
                        "        \"code\": \"venue\"\n" + //
                        "      },\n" + //
                        "      \"valueCodeableConcept\": {\n" + //
                        "        \"coding\": [\n" + //
                        "          {\n" + //
                        "            \"system\": \"http://snomed.info/sct\",\n" + //
                        "            \"code\": \"440655000\",\n" + //
                        "            \"display\": \"Outpatient environment\"\n" + //
                        "          }\n" + //
                        "        ]\n" + //
                        "      }\n" + //
                        "    }\n" + //
                        "  ],\n" + //
                        "  \"jurisdiction\": [\n" + //
                        "    {\n" + //
                        "      \"coding\": [\n" + //
                        "        {\n" + //
                        "          \"system\": \"urn:iso:std:iso:3166\",\n" + //
                        "          \"code\": \"US\"\n" + //
                        "        }\n" + //
                        "      ]\n" + //
                        "    }\n" + //
                        "  ],\n" + //
                        "  \"purpose\": \"This order set helps ensure consistent application of appropriate orders for the care of low suicide risk patients.\",\n" + //
                        "  \"usage\": \"This order set should be applied after assessing a patient for suicide risk, when the findings of that assessment indicate the patient has low suicide risk.\",\n" + //
                        "  \"copyright\": \"© Copyright 2016 Motive Medical Intelligence. All rights reserved.\",\n" + //
                        "  \"approvalDate\": \"2016-03-12\",\n" + //
                        "  \"lastReviewDate\": \"2016-08-15\",\n" + //
                        "  \"effectivePeriod\": {\n" + //
                        "    \"start\": \"2016-01-01\",\n" + //
                        "    \"end\": \"2017-12-31\"\n" + //
                        "  },\n" + //
                        "  \"topic\": [\n" + //
                        "    {\n" + //
                        "      \"text\": \"Suicide risk assessment\"\n" + //
                        "    }\n" + //
                        "  ],\n" + //
                        "  \"author\": [\n" + //
                        "    {\n" + //
                        "      \"name\": \"Motive Medical Intelligence\",\n" + //
                        "      \"telecom\": [\n" + //
                        "        {\n" + //
                        "          \"system\": \"phone\",\n" + //
                        "          \"value\": \"415-362-4007\",\n" + //
                        "          \"use\": \"work\"\n" + //
                        "        },\n" + //
                        "        {\n" + //
                        "          \"system\": \"email\",\n" + //
                        "          \"value\": \"info@motivemi.com\",\n" + //
                        "          \"use\": \"work\"\n" + //
                        "        }\n" + //
                        "      ]\n" + //
                        "    }\n" + //
                        "  ],\n" + //
                        "  \"relatedArtifact\": [\n" + //
                        "    {\n" + //
                        "      \"type\": \"derived-from\",\n" + //
                        "      \"display\": \"Practice Guideline for the Treatment of Patients with Major Depressive Disorder\",\n" + //
                        "      \"url\": \"http://psychiatryonline.org/pb/assets/raw/sitewide/practice_guidelines/guidelines/mdd.pdf\"\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"type\": \"composed-of\",\n" + //
                        "      \"resource\": \"ActivityDefinition/referralPrimaryCareMentalHealth\"\n" + //
                        "    },\n" + //
                        "    {\n" + //
                        "      \"type\": \"composed-of\",\n" + //
                        "      \"resource\": \"ActivityDefinition/citalopramPrescription\"\n" + //
                        "    }\n" + //
                        "  ],\n" + //
                        "  \"library\": [\n" + //
                        "    \"Library/suiciderisk-orderset-logic\"\n" + //
                        "  ],\n" + //
                        "  \"action\": [\n" + //
                        "    {\n" + //
                        "      \"id\": \"suicide-risk-assessment\",\n" + //
                        "      \"title\": \"Suicide Risk Assessment and Outpatient Management\",\n" + //
                        "      \"action\": [\n" + //
                        "        {\n" + //
                        "          \"id\": \"consults-and-referrals\",\n" + //
                        "          \"title\": \"Consults and Referrals\",\n" + //
                        "          \"groupingBehavior\": \"logical-group\",\n" + //
                        "          \"selectionBehavior\": \"any\",\n" + //
                        "          \"action\": [\n" + //
                        "            {\n" + //
                        "              \"id\": \"referral-to-mental-health\",\n" + //
                        "              \"textEquivalent\": \"Refer to outpatient mental health program for evaluation and treatment of mental health conditions now\",\n" + //
                        "              \"definitionCanonical\": \"#referralToMentalHealthCare\",\n" + //
                        "              \"dynamicValue\": [\n" + //
                        "                {\n" + //
                        "                  \"path\": \"timing.event\",\n" + //
                        "                  \"expression\": {\n" + //
                        "                    \"language\": \"text/cql\",\n" + //
                        "                    \"expression\": \"Now()\"\n" + //
                        "                  }\n" + //
                        "                },\n" + //
                        "                {\n" + //
                        "                  \"path\": \"specialty\",\n" + //
                        "                  \"expression\": {\n" + //
                        "                    \"language\": \"text/cql\",\n" + //
                        "                    \"expression\": \"Code '261QM0850X' from SuicideRiskLogic.\\\"NUCC Provider Taxonomy\\\" display 'Adult Mental Health'\"\n" + //
                        "                  }\n" + //
                        "                },\n" + //
                        "                {\n" + //
                        "                  \"path\": \"occurrenceDateTime\",\n" + //
                        "                  \"expression\": {\n" + //
                        "                    \"language\": \"text/cql\",\n" + //
                        "                    \"expression\": \"SuicideRiskLogic.ServiceRequestFulfillmentTime\"\n" + //
                        "                  }\n" + //
                        "                },\n" + //
                        "                {\n" + //
                        "                  \"path\": \"subject\",\n" + //
                        "                  \"expression\": {\n" + //
                        "                    \"language\": \"text/cql\",\n" + //
                        "                    \"expression\": \"SuicideRiskLogic.Patient\"\n" + //
                        "                  }\n" + //
                        "                },\n" + //
                        "                {\n" + //
                        "                  \"path\": \"requester.agent\",\n" + //
                        "                  \"expression\": {\n" + //
                        "                    \"language\": \"text/cql\",\n" + //
                        "                    \"expression\": \"SuicideRiskLogic.Practitioner\"\n" + //
                        "                  }\n" + //
                        "                },\n" + //
                        "                {\n" + //
                        "                  \"path\": \"reasonCode\",\n" + //
                        "                  \"expression\": {\n" + //
                        "                    \"language\": \"text/cql\",\n" + //
                        "                    \"expression\": \"SuicideRiskLogic.RiskAssessmentScore\"\n" + //
                        "                  }\n" + //
                        "                },\n" + //
                        "                {\n" + //
                        "                  \"path\": \"reasonReference\",\n" + //
                        "                  \"expression\": {\n" + //
                        "                    \"language\": \"text/cql\",\n" + //
                        "                    \"expression\": \"SuicideRiskLogic.RiskAssessment\"\n" + //
                        "                  }\n" + //
                        "                }\n" + //
                        "              ]\n" + //
                        "            }\n" + //
                        "          ]\n" + //
                        "        },\n" + //
                        "        {\n" + //
                        "          \"id\": \"medications\",\n" + //
                        "          \"title\": \"Medications\",\n" + //
                        "          \"groupingBehavior\": \"logical-group\",\n" + //
                        "          \"selectionBehavior\": \"at-most-one\",\n" + //
                        "          \"action\": [\n" + //
                        "            {\n" + //
                        "              \"id\": \"first-line-antidepressants\",\n" + //
                        "              \"title\": \"First-Line Antidepressants\",\n" + //
                        "              \"documentation\": [\n" + //
                        "                {\n" + //
                        "                  \"type\": \"citation\",\n" + //
                        "                  \"document\": {\n" + //
                        "                    \"extension\": [\n" + //
                        "                      {\n" + //
                        "                        \"url\": \"http://hl7.org/fhir/StructureDefinition/cqf-qualityOfEvidence\",\n" + //
                        "                        \"valueCodeableConcept\": {\n" + //
                        "                          \"coding\": [\n" + //
                        "                            {\n" + //
                        "                              \"system\": \"http://terminology.hl7.org/CodeSystem/evidence-quality\",\n" + //
                        "                              \"code\": \"high\"\n" + //
                        "                            }\n" + //
                        "                          ],\n" + //
                        "                          \"text\": \"High Quality\"\n" + //
                        "                        }\n" + //
                        "                      }\n" + //
                        "                    ],\n" + //
                        "                    \"contentType\": \"text/html\",\n" + //
                        "                    \"url\": \"http://psychiatryonline.org/pb/assets/raw/sitewide/practice_guidelines/guidelines/mdd.pdf\",\n" + //
                        "                    \"title\": \"Practice Guideline for the Treatment of Patients with Major Depressive Disorder\"\n" + //
                        "                  }\n" + //
                        "                }\n" + //
                        "              ],\n" + //
                        "              \"groupingBehavior\": \"logical-group\",\n" + //
                        "              \"selectionBehavior\": \"at-most-one\",\n" + //
                        "              \"action\": [\n" + //
                        "                {\n" + //
                        "                  \"id\": \"ssri\",\n" + //
                        "                  \"title\": \"Selective Serotonin Reuptake Inhibitors (Choose a mazimum of one or document reasons for exception)\",\n" + //
                        "                  \"documentation\": [\n" + //
                        "                    {\n" + //
                        "                      \"type\": \"citation\",\n" + //
                        "                      \"document\": {\n" + //
                        "                        \"contentType\": \"text/html\",\n" + //
                        "                        \"url\": \"http://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=6daeb45c-451d-b135-bf8f-2d6dff4b6b01\",\n" + //
                        "                        \"title\": \"National Library of Medicine. DailyMed website. CITALOPRAM- citalopram hydrobromide tablet, film coated.\"\n" + //
                        "                      }\n" + //
                        "                    }\n" + //
                        "                  ],\n" + //
                        "                  \"groupingBehavior\": \"logical-group\",\n" + //
                        "                  \"selectionBehavior\": \"at-most-one\",\n" + //
                        "                  \"action\": [\n" + //
                        "                    {\n" + //
                        "                      \"id\": \"citalopram\",\n" + //
                        "                      \"textEquivalent\": \"citalopram 20 mg tablet 1 tablet oral 1 time daily now (30 table; 3 refills)\",\n" + //
                        "                      \"definitionCanonical\": \"#citalopramPrescription\",\n" + //
                        "                      \"dynamicValue\": [\n" + //
                        "                        {\n" + //
                        "                          \"path\": \"status\",\n" + //
                        "                          \"expression\": {\n" + //
                        "                            \"language\": \"text/cql\",\n" + //
                        "                            \"expression\": \"'draft'\"\n" + //
                        "                          }\n" + //
                        "                        },\n" + //
                        "                        {\n" + //
                        "                          \"path\": \"patient\",\n" + //
                        "                          \"expression\": {\n" + //
                        "                            \"language\": \"text/cql\",\n" + //
                        "                            \"expression\": \"SuicideRiskLogic.Patient\"\n" + //
                        "                          }\n" + //
                        "                        },\n" + //
                        "                        {\n" + //
                        "                          \"path\": \"prescriber\",\n" + //
                        "                          \"expression\": {\n" + //
                        "                            \"language\": \"text/cql\",\n" + //
                        "                            \"expression\": \"SuicideRiskLogic.Practitioner\"\n" + //
                        "                          }\n" + //
                        "                        },\n" + //
                        "                        {\n" + //
                        "                          \"path\": \"reasonCode\",\n" + //
                        "                          \"expression\": {\n" + //
                        "                            \"language\": \"text/cql\",\n" + //
                        "                            \"expression\": \"SuicideRiskLogic.RiskAssessmentScore\"\n" + //
                        "                          }\n" + //
                        "                        },\n" + //
                        "                        {\n" + //
                        "                          \"path\": \"reasonReference\",\n" + //
                        "                          \"expression\": {\n" + //
                        "                            \"language\": \"text/cql\",\n" + //
                        "                            \"expression\": \"SuicideRiskLogic.RiskAssessment\"\n" + //
                        "                          }\n" + //
                        "                        }\n" + //
                        "                      ]\n" + //
                        "                    },\n" + //
                        "                    {\n" + //
                        "                      \"id\": \"escitalopram\",\n" + //
                        "                      \"textEquivalent\": \"escitalopram 10 mg tablet 1 tablet oral 1 time daily now (30 tablet; 3 refills)\"\n" + //
                        "                    },\n" + //
                        "                    {\n" + //
                        "                      \"id\": \"fluoxetine\",\n" + //
                        "                      \"textEquivalent\": \"fluoxetine 20 mg capsule 1 capsule oral 1 time daily now (30 tablet; 3 refills)\"\n" + //
                        "                    },\n" + //
                        "                    {\n" + //
                        "                      \"id\": \"paroxetine\",\n" + //
                        "                      \"textEquivalent\": \"paroxetine 20 mg tablet 1 tablet oral 1 time daily now (30 tablet; 3 refills)\"\n" + //
                        "                    },\n" + //
                        "                    {\n" + //
                        "                      \"id\": \"sertraline\",\n" + //
                        "                      \"textEquivalent\": \"sertraline 50 mg tablet 1 tablet oral 1 time daily now (30 tablet; 3 refills)\"\n" + //
                        "                    }\n" + //
                        "                  ]\n" + //
                        "                },\n" + //
                        "                {\n" + //
                        "                  \"id\": \"dnri\",\n" + //
                        "                  \"textEquivalent\": \"Dopamine Norepinephrine Reuptake Inhibitors (Choose a maximum of one or document reasons for exception)\"\n" + //
                        "                },\n" + //
                        "                {\n" + //
                        "                  \"id\": \"snri\",\n" + //
                        "                  \"textEquivalent\": \"Serotonin Norepinephrine Reuptake Inhibitors (Choose a maximum of one or doument reasons for exception)\"\n" + //
                        "                },\n" + //
                        "                {\n" + //
                        "                  \"id\": \"nsm\",\n" + //
                        "                  \"textEquivalent\": \"Norepinephrine-Serotonin Modulators (Choose a maximum of one or document reasons for exception)\"\n" + //
                        "                }\n" + //
                        "              ]\n" + //
                        "            }\n" + //
                        "          ]\n" + //
                        "        }\n" + //
                        "      ]\n" + //
                        "    }\n" + //
                        "  ]\n" + //
                        "}"; // Replace with actual raw PlanDefinition
        String bpmn = bpmnBuilder.planDefinitionToBpmn(rawPlanDefinition.trim());
        System.out.println(bpmn);
        assertNotNull(bpmn);
        assertTrue(bpmn.contains("definitions"), "La salida debería ser un documento BPMN");
    }
}
