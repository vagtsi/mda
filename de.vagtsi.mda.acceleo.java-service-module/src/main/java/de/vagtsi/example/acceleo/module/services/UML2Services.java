package de.vagtsi.example.acceleo.module.services;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.uml2.uml.Stereotype;

public class UML2Services {
	private static Logger logger = Logger.getLogger(UML2Services.class.getName());
	private static final String SOMDA_NAMESPACE = "somda::";
	private static final String MAPPINGS_FILENAME = "/mappings/JavaMappings.xml";

	// the xml mapping file tags
	private static final String MAPPING_TAG = "mapping";
	private static final String FROM_TAG = "from";
	private static final String TO_TAG = "to";

	//initialize type mappings from resource file
	private final boolean addSomdaNamespace = true;
	final Map<String, String> types = getTypeMappings();

	public boolean hasStereotype(org.eclipse.uml2.uml.Class clazz, String stereotypeName) {
		List<Stereotype> stereotypes = clazz.getAppliedStereotypes();
		for (Stereotype stereotype : stereotypes) {
			if (stereotype.getName().equals(stereotypeName)) {
				return true;
			}
		}
		return false;
	}

	public String getQualifiedJavaName(org.eclipse.uml2.uml.Type type) {
		String javaType = types.get(type.getQualifiedName());
		if (javaType != null) {
			return javaType;
		} else {
			throw new IllegalArgumentException(
					String.format("Unsupported type '%s' in model", type.getQualifiedName()));
		}
	}
	
	private Map<String, String> getTypeMappings() {
		logger.info(String.format("Reading type mappings from resource '%s'", MAPPINGS_FILENAME));

		// read the xml type mapping file
		InputStream mappingResourceFile = this.getClass().getResourceAsStream(MAPPINGS_FILENAME);
		if (mappingResourceFile == null) {
			throw new IllegalArgumentException(
					String.format("Mapping resource file '%s' not found", MAPPINGS_FILENAME));
		}

		Map<String, String> map = new HashMap<>(); // the resulting type map

		XMLInputFactory factory = XMLInputFactory.newInstance();
		try {
			XMLStreamReader streamReader = factory.createXMLStreamReader(mappingResourceFile);

			Set<String> datatypes = new HashSet<>(); // the current 'from' (key) mapping types
			String javatype = null; // the resulting java type ('to')

			while (streamReader.hasNext()) {
				int type = streamReader.next();

				switch (type) {
				case XMLStreamReader.START_ELEMENT:
					switch (streamReader.getLocalName()) {
					case FROM_TAG:
						datatypes.add(streamReader.getElementText());
						break;
					case TO_TAG:
						javatype = streamReader.getElementText();
						break;
					}
					break;
				case XMLStreamReader.END_ELEMENT:
					switch (streamReader.getLocalName()) {
					case MAPPING_TAG:
						if (javatype != null) {
							for (String datatype : datatypes) {
								if (addSomdaNamespace && !datatype.startsWith(SOMDA_NAMESPACE)) {
									datatype = SOMDA_NAMESPACE + datatype;
								}
								map.put(datatype, javatype);
							}
						}
						datatypes.clear();
						javatype = null;
						break;
					}
					break;
				}

			}
		} catch (XMLStreamException e) {
			throw new IllegalArgumentException("Failed to load mapping resource properties file", e);
		}

		logger.info(String.format("- intialized with %s type mappings: %s", map.size(), map.keySet().toString()));
		return map;
	}

}
