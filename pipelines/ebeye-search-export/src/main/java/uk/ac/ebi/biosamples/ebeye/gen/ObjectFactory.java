/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.ebeye.gen;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface
 * generated in the uk.ac.ebi.biosamples.ebeye.gen package.
 *
 * <p>An ObjectFactory allows you to programatically construct new instances of the Java
 * representation for XML content. The Java representation of XML content can consist of schema
 * derived interfaces and classes representing the binding of schema type definitions, element
 * declarations and model groups. Factory methods for each of these are provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

  private static final QName _Database_QNAME = new QName("", "database");

  /**
   * Create a new ObjectFactory that can be used to create new instances of schema derived classes
   * for package: uk.ac.ebi.biosamples.ebeye.gen
   */
  public ObjectFactory() {}

  /** Create an instance of {@link HierarchicalValueType } */
  public HierarchicalValueType createHierarchicalValueType() {
    return new HierarchicalValueType();
  }

  /** Create an instance of {@link DatabaseType } */
  public DatabaseType createDatabaseType() {
    return new DatabaseType();
  }

  /** Create an instance of {@link EntryType } */
  public EntryType createEntryType() {
    return new EntryType();
  }

  /** Create an instance of {@link DateType } */
  public DateType createDateType() {
    return new DateType();
  }

  /** Create an instance of {@link CrossReferencesType } */
  public CrossReferencesType createCrossReferencesType() {
    return new CrossReferencesType();
  }

  /** Create an instance of {@link RefType } */
  public RefType createRefType() {
    return new RefType();
  }

  /** Create an instance of {@link DatesType } */
  public DatesType createDatesType() {
    return new DatesType();
  }

  /** Create an instance of {@link AdditionalFieldsType } */
  public AdditionalFieldsType createAdditionalFieldsType() {
    return new AdditionalFieldsType();
  }

  /** Create an instance of {@link FieldType } */
  public FieldType createFieldType() {
    return new FieldType();
  }

  /** Create an instance of {@link EntriesType } */
  public EntriesType createEntriesType() {
    return new EntriesType();
  }

  /** Create an instance of {@link HierarchicalValueType.Root } */
  public HierarchicalValueType.Root createHierarchicalValueTypeRoot() {
    return new HierarchicalValueType.Root();
  }

  /** Create an instance of {@link HierarchicalValueType.Child } */
  public HierarchicalValueType.Child createHierarchicalValueTypeChild() {
    return new HierarchicalValueType.Child();
  }

  /** Create an instance of {@link JAXBElement }{@code <}{@link DatabaseType }{@code >}} */
  @XmlElementDecl(namespace = "", name = "database")
  public JAXBElement<DatabaseType> createDatabase(DatabaseType value) {
    return new JAXBElement<DatabaseType>(_Database_QNAME, DatabaseType.class, null, value);
  }
}
