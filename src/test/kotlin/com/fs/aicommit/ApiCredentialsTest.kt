package com.fs.aicommit

import com.intellij.util.xmlb.XmlSerializer
import org.jdom.output.XMLOutputter
import org.junit.Assert.*
import org.junit.Test

class ApiCredentialsTest {
    @Test
    fun `API key is excluded from serialized settings`() {
        val settings = AiCommitSettings().apply {
            apiConfigs.add(ApiProviderConfig(name = "Test", apiKey = "private-secret", model = "model"))
        }
        val xml = XMLOutputter().outputString(XmlSerializer.serialize(settings))
        assertFalse(xml.contains("private-secret"))
        assertFalse(xml.contains("apiKey"))
    }
}
