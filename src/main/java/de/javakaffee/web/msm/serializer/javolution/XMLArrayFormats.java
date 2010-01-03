/*
 * Copyright 2009 Martin Grotzke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.javakaffee.web.msm.serializer.javolution;

import java.lang.reflect.Array;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.javakaffee.web.msm.serializer.javolution.XMLBinding.InputElement;
import de.javakaffee.web.msm.serializer.javolution.XMLBinding.OutputElement;
import de.javakaffee.web.msm.serializer.javolution.XMLBinding.XMLFormat;

/**
 * A class that collects different {@link XMLFormat} implementations for arrays.
 * 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public class XMLArrayFormats {

    static final XMLFormat<boolean[]> BOOLEAN_ARRAY_FORMAT = new XMLFormat<boolean[]>() {

        @Override
        public boolean[] newInstance( final Class<boolean[]> clazz, final InputElement input ) throws XMLStreamException {
            try {
                final int length = input.getAttribute( "length", 0 );
                return (boolean[]) Array.newInstance( boolean.class, length );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

        @Override
        public void read( final InputElement input, final boolean[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getAttribute( "v", false );
                input.endElement();
            }
        }

        @Override
        public final void write( final boolean[] array, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "length", array.length );
            final XMLStreamWriter streamWriter = output.getStreamWriter();
            for ( final boolean item : array ) {
                streamWriter.writeStartElement( "i.d.t" );
                output.setAttribute( "v", item );
                streamWriter.writeEndElement();
            }
        }

    };

    static final XMLFormat<byte[]> BYTE_ARRAY_FORMAT = new XMLFormat<byte[]>() {

        @Override
        public byte[] newInstance( final Class<byte[]> clazz, final InputElement input ) throws XMLStreamException {
            try {
                final int length = input.getAttribute( "length", 0 );
                return (byte[]) Array.newInstance( byte.class, length );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

        @Override
        public void read( final InputElement input, final byte[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getAttribute( "v", (byte)0 );
                input.endElement();
            }
        }

        @Override
        public final void write( final byte[] array, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "length", array.length );
            final XMLStreamWriter streamWriter = output.getStreamWriter();
            for ( final byte item : array ) {
                streamWriter.writeStartElement( "i" );
                output.setAttribute( "v", item );
                streamWriter.writeEndElement();
            }
        }

    };

    static final XMLFormat<char[]> CHAR_ARRAY_FORMAT = new XMLFormat<char[]>() {

        @Override
        public char[] newInstance( final Class<char[]> clazz, final InputElement input ) throws XMLStreamException {
            try {
                final int length = input.getAttribute( "length", 0 );
                return (char[]) Array.newInstance( char.class, length );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

        @Override
        public void read( final InputElement input, final char[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getAttribute( "v", (char)0 );
                input.endElement();
            }
        }

        @Override
        public final void write( final char[] array, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "length", array.length );
            final XMLStreamWriter streamWriter = output.getStreamWriter();
            for ( final char item : array ) {
                streamWriter.writeStartElement( "i" );
                output.setAttribute( "v", item );
                streamWriter.writeEndElement();
            }
        }

    };

    static final XMLFormat<short[]> SHORT_ARRAY_FORMAT = new XMLFormat<short[]>() {

        @Override
        public short[] newInstance( final Class<short[]> clazz, final InputElement input ) throws XMLStreamException {
            try {
                final int length = input.getAttribute( "length", 0 );
                return (short[]) Array.newInstance( short.class, length );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

        @Override
        public void read( final InputElement input, final short[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getAttribute( "v", (short)0 );
                input.endElement();
            }
        }

        @Override
        public final void write( final short[] array, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "length", array.length );
            final XMLStreamWriter streamWriter = output.getStreamWriter();
            for ( final short item : array ) {
                streamWriter.writeStartElement( "i" );
                output.setAttribute( "v", item );
                streamWriter.writeEndElement();
            }
        }

    };

    static final XMLFormat<int[]> INT_ARRAY_FORMAT = new XMLFormat<int[]>() {

        @Override
        public int[] newInstance( final Class<int[]> clazz, final InputElement input ) throws XMLStreamException {
            try {
                final int length = input.getAttribute( "length", 0 );
                return (int[]) Array.newInstance( int.class, length );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

        @Override
        public void read( final InputElement input, final int[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getAttribute( "v", 0 );
                input.endElement();
            }
        }

        @Override
        public final void write( final int[] array, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "length", array.length );
            final XMLStreamWriter streamWriter = output.getStreamWriter();
            for ( final int item : array ) {
                streamWriter.writeStartElement( "i" );
                output.setAttribute( "v", item );
                streamWriter.writeEndElement();
            }
        }

    };

    static final XMLFormat<long[]> LONG_ARRAY_FORMAT = new XMLFormat<long[]>() {

        @Override
        public long[] newInstance( final Class<long[]> clazz, final InputElement input ) throws XMLStreamException {
            try {
                final int length = input.getAttribute( "length", 0 );
                return (long[]) Array.newInstance( long.class, length );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

        @Override
        public void read( final InputElement input, final long[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getAttribute( "v", 0L );
                input.endElement();
            }
        }

        @Override
        public final void write( final long[] array, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "length", array.length );
            final XMLStreamWriter streamWriter = output.getStreamWriter();
            for ( final long item : array ) {
                streamWriter.writeStartElement( "i" );
                output.setAttribute( "v", item );
                streamWriter.writeEndElement();
            }
        }

    };

    static final XMLFormat<float[]> FLOAT_ARRAY_FORMAT = new XMLFormat<float[]>() {

        @Override
        public float[] newInstance( final Class<float[]> clazz, final InputElement input ) throws XMLStreamException {
            try {
                final int length = input.getAttribute( "length", 0 );
                return (float[]) Array.newInstance( float.class, length );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

        @Override
        public void read( final InputElement input, final float[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getAttribute( "v", 0f );
                input.endElement();
            }
        }

        @Override
        public final void write( final float[] array, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "length", array.length );
            final XMLStreamWriter streamWriter = output.getStreamWriter();
            for ( final float item : array ) {
                streamWriter.writeStartElement( "i" );
                output.setAttribute( "v", item );
                streamWriter.writeEndElement();
            }
        }

    };

    static final XMLFormat<double[]> DOUBLE_ARRAY_FORMAT = new XMLFormat<double[]>() {

        @Override
        public double[] newInstance( final Class<double[]> clazz, final InputElement input ) throws XMLStreamException {
            try {
                final int length = input.getAttribute( "length", 0 );
                return (double[]) Array.newInstance( double.class, length );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

        @Override
        public void read( final InputElement input, final double[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getAttribute( "v", 0d );
                input.endElement();
            }
        }

        @Override
        public final void write( final double[] array, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "length", array.length );
            final XMLStreamWriter streamWriter = output.getStreamWriter();
            for ( final double item : array ) {
                streamWriter.writeStartElement( "i" );
                output.setAttribute( "v", item );
                streamWriter.writeEndElement();
            }
        }

    };

}