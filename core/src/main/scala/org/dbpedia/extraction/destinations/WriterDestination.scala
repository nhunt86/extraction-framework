package org.dbpedia.extraction.destinations

import java.io.Writer

import org.apache.jena.shared.BadURIException
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.mappings.ExtractionRecorder
import org.dbpedia.extraction.transform.Quad

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
 * Writes quads to a writer.
 * 
 * @param called in open() to obtain the writer.
 */
class WriterDestination(factory: () => Writer, formatter : Formatter, extractionRecorder: ExtractionRecorder[Quad] = null)
extends Destination
{
  private var writer: Writer = null
  
  override def open() = {
    if(writer == null) //to prevent errors when called twice
    {
      writer = factory()
      writer.write(formatter.header)
    }
  }
  
  /**
   * Note: using synchronization here is not strictly necessary (writers should be thread-safe),
   * but without it, different sequences of quads will be interleaved, which is harder to read
   * and makes certain parsing optimizations impossible.
   */
  override def write(graph : Traversable[Quad]) = synchronized {
    for(quad <- graph) {
      val formatted = formatter.render(quad)
      if(extractionRecorder != null) if(formatted.startsWith("#")) extractionRecorder.failedRecord(quad.subject, null, quad, new BadURIException(formatted))
      writer.write(formatted)
    }
  }

  override def close() = {
    if(writer != null) {
      writer.write(formatter.footer)
      writer.close()
    }
  }
}
