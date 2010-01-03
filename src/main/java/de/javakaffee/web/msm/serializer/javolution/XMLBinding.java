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

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.codec.EncoderException;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.aalto.stax.OutputFactoryImpl;

/**
 * An {@link XMLBinding} that provides class bindings based on reflection.
 * 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public class XMLBinding {
    
    private static final Logger _log = Logger.getLogger( XMLBinding.class.getName() );
    private static final OutputFactoryImpl _outputFactory;
    private static final InputFactoryImpl _inputFactory;
    
    private static final XMLEnumFormat ENUM_FORMAT = new XMLEnumFormat();
    private static final XMLCalendarFormat XML_CALENDAR = new XMLCalendarFormat();
    private static final XMLArrayFormat ARRAY_FORMAT = new XMLArrayFormat();
    private static final Map<Class<?>, XMLFormat<?>> _formats = new ConcurrentHashMap<Class<?>, XMLFormat<?>>();

    private static final String ID = "__id";
    private static final String REF = "ref";
    
    static {
        _outputFactory = (OutputFactoryImpl) com.fasterxml.aalto.stax.OutputFactoryImpl.newInstance();
        _outputFactory.configureForSpeed();
        
        _inputFactory = (InputFactoryImpl) com.fasterxml.aalto.stax.InputFactoryImpl.newInstance();
        _inputFactory.configureForSpeed();
    }
    
    public XMLBinding() {
    }
    
    class XMLWriter implements Closeable {

        private final XMLStreamWriter _streamWriter;

        /**
         * @param streamWriter
         */
        public XMLWriter( final XMLStreamWriter streamWriter ) {
            _streamWriter = streamWriter;
        }

        /**
         */
        public void close() {
            try {
                _streamWriter.close();
            } catch ( final Exception e ) {
                _log.warning( "Could not close XmlStreamWriter: " + e );
            }
        }

        /**
         * @param o
         * @throws XMLStreamException 
         */
        public void write( final Object o ) throws XMLStreamException {

            _streamWriter.writeStartDocument();
            new OutputElement( _streamWriter ).add( o, "root" );
            
            _streamWriter.writeEndDocument();
        }

        /**
         * @throws XMLStreamException 
         * 
         */
        public void flush() throws XMLStreamException {
            _streamWriter.flush();
        }
        
    }
    
    class XMLReader implements Closeable {

        private final XMLStreamReader _streamReader;

        /**
         * @param streamWriter
         */
        public XMLReader( final XMLStreamReader streamReader ) {
            _streamReader = streamReader;
        }

        /**
         */
        public void close() {
            try {
                _streamReader.close();
            } catch ( final Exception e ) {
                _log.warning( "Could not close XmlStreamWriter: " + e );
            }
        }

        /**
         * @param o
         * @param name
         * @throws XMLStreamException 
         */
        @SuppressWarnings( "unchecked" )
        public <T> T read() throws XMLStreamException {
            final InputElement inputElement = new InputElement( _streamReader );
            return (T) inputElement.get( "root" );
        }
        
    }

    /**
     * @param bos
     * @return 
     * @throws XMLStreamException 
     */
    public XMLWriter newXMLWriter( final OutputStream out ) throws XMLStreamException {
        // TODO Auto-generated method stub
        final XMLStreamWriter streamWriter = _outputFactory.createXMLStreamWriter( out );
        return new XMLWriter( streamWriter );
    }
    
    public XMLReader newXMLReader( final InputStream in ) throws XMLStreamException {
        // TODO Auto-generated method stub
        final XMLStreamReader streamReader = _inputFactory.createXMLStreamReader( in );
        return new XMLReader( streamReader );
    }

    @SuppressWarnings( "unchecked" )
    public <T> XMLFormat<T> getFormat(final Class<? extends T> cls) {
        XMLFormat<?> xmlFormat = _formats.get( cls );
        if ( xmlFormat != null ) {
            return (XMLFormat<T>) xmlFormat;
        }
        
//        //System.out.println( "got format " + format + " for class " +  cls);
//        if ( cls.isPrimitive() || cls.equals( String.class ) || Number.class.isAssignableFrom( cls )
//                || Map.class.isAssignableFrom( cls ) || Collection.class.isAssignableFrom( cls ) )
//            return format;
        if ( cls == Boolean.class ) {
            return (XMLFormat<T>) XML_BOOLEAN;
        }
        else if ( cls == String.class ) {
            return (XMLFormat<T>) XML_STRING;
        }
        else if ( cls == Character.class ) {
            return (XMLFormat<T>) XML_CHARACTER;
        }
        else if ( cls == Byte.class ) {
            return (XMLFormat<T>) XML_BYTE;
        }
        else if ( cls == Short.class ) {
            return (XMLFormat<T>) XML_SHORT;
        }
        else if ( cls == Integer.class ) {
            return (XMLFormat<T>) XML_INTEGER;
        }
        else if ( cls == Long.class ) {
            return (XMLFormat<T>) XML_LONG;
        }
        else if ( cls == Float.class ) {
            return (XMLFormat<T>) XML_FLOAT;
        }
        else if ( cls == Double.class ) {
            return (XMLFormat<T>) XML_DOUBLE;
        }
        if ( cls.isArray() ) {
            return getArrayFormat( cls );
        }
        else if ( cls.isEnum() ) {
            return (XMLFormat<T>) ENUM_FORMAT;
        }
        else if ( Collection.class.isAssignableFrom( cls ) ) {
            return (XMLFormat<T>) XMLCollectionFormat;
        }
        else if ( Map.class.isAssignableFrom( cls ) ) {
            return (XMLFormat<T>) XMLMapFormat;
        }
        else if ( cls == Class.class ) {
            return (XMLFormat<T>) XML_CLASS;
        }
        else if ( Calendar.class.isAssignableFrom( cls ) ) {
            return (XMLFormat<T>) XML_CALENDAR;
        }
        else {
            if ( xmlFormat == null ) {
                if ( XMLReflectionFormat.isNumberFormat( cls ) ) {
                    xmlFormat = XMLReflectionFormat.getNumberFormat( cls );
                } else {
                    xmlFormat = new XMLReflectionFormat( cls );
                }
                _formats.put( cls, xmlFormat );
            }
            return (XMLFormat<T>) xmlFormat;
        }
    }

    @SuppressWarnings( "unchecked" )
    private XMLFormat getArrayFormat( final Class cls ) {
        if ( cls == int[].class ) {
            return XMLArrayFormats.INT_ARRAY_FORMAT;
        } else if ( cls == long[].class ) {
            return XMLArrayFormats.LONG_ARRAY_FORMAT;
        } else if ( cls == short[].class ) {
            return XMLArrayFormats.SHORT_ARRAY_FORMAT;
        } else if ( cls == float[].class ) {
            return XMLArrayFormats.FLOAT_ARRAY_FORMAT;
        } else if ( cls == double[].class ) {
            return XMLArrayFormats.DOUBLE_ARRAY_FORMAT;
        } else if ( cls == char[].class ) {
            return XMLArrayFormats.CHAR_ARRAY_FORMAT;
        } else if ( cls == byte[].class ) {
            return XMLArrayFormats.BYTE_ARRAY_FORMAT;
        } else if ( cls == boolean[].class ) {
            return XMLArrayFormats.BOOLEAN_ARRAY_FORMAT;
        } else {
            return ARRAY_FORMAT;
        }
    }
    
    static abstract class XMLFormat<T> {

        protected T newInstance( final Class<T> clazz, final InputElement in ) throws XMLStreamException {
            try {
                return clazz.newInstance();
            } catch ( final InstantiationException e ) {
                throw new XMLStreamException( e );
            } catch ( final IllegalAccessException e ) {
                throw new XMLStreamException( e );
            }
        }

        /**
         * @param o
         * @param streamWriter
         * @throws XMLStreamException 
         */
        abstract void write( T o, OutputElement out ) throws XMLStreamException;

        abstract void read( final InputElement in, final T obj ) throws XMLStreamException;
        
    }
    
    class OutputElement {
        
        private final Map<Object, Integer> _referenceMap = new IdentityHashMap<Object, Integer>( 50 );
        private final XMLStreamWriter _streamWriter;
        private int _idSeq = 0;
        /**
         * @param streamWriter
         */
        public OutputElement( final XMLStreamWriter streamWriter ) {
            _streamWriter = streamWriter;
        }
        
        public void setAttribute( final String name, final String value ) throws XMLStreamException {
            _streamWriter.writeAttribute( name, value );
        }
        public void setAttribute( final String name, final byte value ) throws XMLStreamException {
            _streamWriter.writeAttribute( name, String.valueOf( value ) );
        }
        public void setAttribute( final String name, final char value ) throws XMLStreamException {
            _streamWriter.writeAttribute( name, String.valueOf( value ) );
        }
        public void setAttribute( final String name, final short value ) throws XMLStreamException {
            _streamWriter.writeAttribute( name, String.valueOf( value ) );
        }
        public void setAttribute( final String name, final int value ) throws XMLStreamException {
            _streamWriter.writeAttribute( name, String.valueOf( value ) );
        }
        public void setAttribute( final String name, final long value ) throws XMLStreamException {
            _streamWriter.writeAttribute( name, String.valueOf( value ) );
        }
        public void setAttribute( final String name, final float value ) throws XMLStreamException {
            _streamWriter.writeAttribute( name, String.valueOf( value ) );
        }
        public void setAttribute( final String name, final double value ) throws XMLStreamException {
            _streamWriter.writeAttribute( name, String.valueOf( value ) );
        }
        public void setAttribute( final String name, final boolean value ) throws XMLStreamException {
            _streamWriter.writeAttribute( name, String.valueOf( value ) );
        }

        public void add( final Object obj ) throws XMLStreamException {
            
            if ( obj == null ) {
                // TODO: unit test this, also with reading (the null needs to be read/set accordingly) 
                return;
            }
            
            final Integer id = _referenceMap.get( obj );
            if ( id != null ) {
                _streamWriter.writeAttribute( REF, id.toString() );
            }
            else {
                _streamWriter.writeAttribute( "class", obj.getClass().getName() );
                final int newId = _idSeq++;
                _referenceMap.put( obj, newId );
                _streamWriter.writeAttribute( ID, String.valueOf( newId ) );
                getFormat( obj.getClass() ).write( obj, this );
            }
        }
        
        
        public void add( final Object object, final String name ) throws XMLStreamException {
            
            _streamWriter.writeStartElement( replace( name, '$', "_d_" ) );
            add( object );
            _streamWriter.writeEndElement();
//
//            _streamWriter.writeStartElement( name );
//            add( object );
//            _streamWriter.writeEndElement();
        }

        /**
         * @return
         */
        public XMLStreamWriter getStreamWriter() {
            return _streamWriter;
        }
        
    }
    
    static String replace( final String text, final char c, final String chars ) {
        int lastPos = 0;
        StringBuilder sb = null;
        for( int i = 0; i < text.length(); i++ ) {
            if ( text.charAt( i ) == c ) {
                if ( sb == null ) {
                    sb = new StringBuilder();
                }
                sb.append( text.subSequence( lastPos, i ) ).append( chars );
                lastPos = i+1;
            }
        }
        if ( sb != null ) {
            sb.append( text.subSequence( lastPos, text.length() ) );
        }
        return sb != null ? sb.toString() : text;
    }
    
    public static void main( final String[] args ) throws UnsupportedEncodingException, EncoderException {
        Pattern.compile( "\\$" ).matcher( "this$0" ).replaceAll( "_d_" );
        
        System.out.println( new String((null+":this$0").getBytes("UTF-8")) );
        System.out.println( URLEncoder.encode( new String((null+":this $").getBytes("UTF-8")) ) );
        System.out.println( "this$0".replaceAll( "\\$", "_d_" ) );
        System.out.println( Pattern.compile( "\\$" ).matcher( "this$0" ).replaceAll( "_d_" ) );
        System.out.println( Pattern.compile( "_d_" ).matcher( "this_d_0" ).replaceAll( "\\$" ) );
    }

    private static final Pattern PO = Pattern.compile( "\\$" );
    private static final Pattern PI = Pattern.compile( "_d_" );
    
    class InputElement {
        
        private final Map<String, Object> _referenceMap = new HashMap<String, Object>( 50 );
        private final XMLStreamReader _reader;
        
        private boolean _next;
        
        public InputElement( final XMLStreamReader reader ) {
            _reader = reader;
        }

        public String getAttribute( final String name ) throws XMLStreamException {
            return _reader.getAttributeValue( null, name );
        }

        public String getAttribute( final String name, final String defaultValue ) throws XMLStreamException {
            final String result = _reader.getAttributeValue( null, name );
            return result != null ? result : defaultValue;
        }
        
        public byte getAttribute( final String name, final byte defaultValue ) throws XMLStreamException {
            final String result = _reader.getAttributeValue( null, name );
            return result != null ? Byte.parseByte( result ) : defaultValue;
        }
        
        public char getAttribute( final String name, final char defaultValue ) throws XMLStreamException {
            final String result = _reader.getAttributeValue( null, name );
            if ( result != null ) {
                if ( result.length() > 1 ) {
                    throw new XMLStreamException( "The attribute '" + name + "' of type Character has illegal value (length > 1): " + result );
                }
                return Character.valueOf( result.charAt( 0 ) );
            }
            return defaultValue;
        }
        
        public short getAttribute( final String name, final short defaultValue ) throws XMLStreamException {
            final String result = _reader.getAttributeValue( null, name );
            return result != null ? Short.parseShort( result ) : defaultValue;
        }

        public int getAttribute( final String name, final int defaultValue ) throws XMLStreamException {
            final String result = _reader.getAttributeValue( null, name );
            return result != null ? Integer.parseInt( result ) : defaultValue;
        }

        public long getAttribute( final String name, final long defaultValue ) throws XMLStreamException {
            final String result = _reader.getAttributeValue( null, name );
            return result != null ? Long.parseLong( result ) : defaultValue;
        }
        
        public boolean getAttribute( final String name, final boolean defaultValue ) throws XMLStreamException {
            final String result = _reader.getAttributeValue( null, name );
            return result != null ? Boolean.parseBoolean( result ) : defaultValue;
        }
        
        public float getAttribute( final String name, final float defaultValue ) throws XMLStreamException {
            final String result = _reader.getAttributeValue( null, name );
            return result != null ? Float.parseFloat( result ) : defaultValue;
        }
        
        public double getAttribute( final String name, final double defaultValue ) throws XMLStreamException {
            final String result = _reader.getAttributeValue( null, name );
            return result != null ? Double.parseDouble( result ) : defaultValue;
        }
        
        public int getAttributeCount() {
            return _reader.getAttributeCount();
        }
        
        public String getAttributeName( final int i ) {
            return _reader.getAttributeLocalName( i );
        }
        
        public String getAttributeValue( final int i ) {
            return _reader.getAttributeValue( i );
        }

        /**
         * @return
         * @throws XMLStreamException 
         */
        public boolean hasNext() throws XMLStreamException {
            if ( !_next ) {
                _next = true;
                _reader.nextTag();
            }
            return _reader.getEventType() == XMLStreamReader.START_ELEMENT;
        }

        /**
         * Scrolls to the end of this element. Is required if {@link #hasNext()} was checked,
         * an attribute was read from the element and the next element shall be
         * available for {@link #hasNext()}.
         * @return <code>true</code> if we're at the end of the element.
         * @throws XMLStreamException 
         */
        public boolean endElement() throws XMLStreamException {
            // TODO: unit test this implementation if it does what's expected
            // in combination with hasNext and getNext etc.
            if ( _next ) {
                _next = false;
                _reader.nextTag();
            }
            return _reader.getEventType() == XMLStreamReader.END_ELEMENT;
        }

        /**
         * @return
         * @throws XMLStreamException 
         */
        @SuppressWarnings( "unchecked" )
        public Object getNext() throws XMLStreamException {
            final String ref = _reader.getAttributeValue( null, REF );
            if ( ref != null ) {
                if ( _reader.next() != XMLStreamReader.END_ELEMENT ) {
                    throw new XMLStreamException("Non Empty Reference Element", _reader.getLocation());
                }
                _next = false;
                return _referenceMap.get( ref );
            }
            
            final String className = _reader.getAttributeValue( null, "class" );
            try {
                final Class<?> clazz = Class.forName( className );
                final XMLFormat<Object> format = getFormat( clazz );
                _next = false;
                final Object object = format.newInstance( (Class<Object>) clazz, this );
                _referenceMap.put( _reader.getAttributeValue( null, ID ), object );
                format.read( this, object );
                if (hasNext()) {
                    throw new XMLStreamException("Incomplete element reading", _reader.getLocation());
                }
                _next = false;
                return object;
            } catch ( final ClassNotFoundException e ) {
                throw new XMLStreamException( e );
            }
        }

        /**
         * @return
         * @throws XMLStreamException 
         */
        public Element getNextElement() throws XMLStreamException {
            if ( !hasNext() ) {
                throw new XMLStreamException("No more element to read", _reader.getLocation());
            }
            final String localName = _reader.getLocalName();
            return new Element( PI.matcher( localName ).replaceAll( "\\$" ), getNext() );
        }

        /**
         * @param name
         * @return
         * @throws XMLStreamException 
         */
        public Object get( final String name ) throws XMLStreamException {
            if ( !hasNext() || !_reader.getLocalName().equals( name ) ) {
                return null;
            }
            return getNext();
        }
        
    }
    
    static final class Element {
        final String name;
        final Object object;
        public Element( final String name, final Object object ) {
            this.name = name;
            this.object = object;
        }
    }

    public static final XMLFormat<String> XML_STRING = new XMLFormat<String>() {
        
        @Override
        protected String newInstance(final java.lang.Class<String> clazz, final InputElement in) throws XMLStreamException {
            return in.getAttribute( "v", null );
        };
        
        @Override
        public void read( final InputElement xml, final String obj ) throws XMLStreamException {
            // nothing to do
        }
        
        @Override
        public void write( final String obj, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "v", obj );
        }
        
    };

    public static final XMLFormat<Class<?>> XML_CLASS = new XMLFormat<Class<?>>() {
        
        @Override
        protected Class<?> newInstance(final java.lang.Class<Class<?>> clazz, final InputElement in) throws XMLStreamException {
            try {
                return Class.forName( in.getAttribute( "name", null ) );
            } catch ( final ClassNotFoundException e ) {
                throw new XMLStreamException( e );
            }
        };
        
        @Override
        public void read( final InputElement xml, final Class<?> obj ) throws XMLStreamException {
            // nothing to do
        }
        
        @Override
        public void write( final Class<?> obj, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "name", obj.getName() );
        }
        
    };

    public static final XMLFormat<Boolean> XML_BOOLEAN = new XMLFormat<Boolean>() {
        
        @Override
        protected Boolean newInstance(final java.lang.Class<Boolean> clazz, final InputElement in) throws XMLStreamException {
            return Boolean.valueOf( in.getAttribute( "v" ) );
        };
        
        @Override
        public void read( final InputElement xml, final Boolean obj ) throws XMLStreamException {
            // nothing to do
        }
        
        @Override
        public void write( final Boolean obj, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "v", obj.toString() );
        }
        
    };

    public static final XMLFormat<Short> XML_SHORT = new XMLFormat<Short>() {
        
        @Override
        protected Short newInstance(final java.lang.Class<Short> clazz, final InputElement in) throws XMLStreamException {
            return Short.valueOf( in.getAttribute( "v" ) );
        };
        
        @Override
        public void read( final InputElement xml, final Short obj ) throws XMLStreamException {
            // nothing to do
        }
        
        @Override
        public void write( final Short obj, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "v", obj.toString() );
        }
        
    };

    public static final XMLFormat<Integer> XML_INTEGER = new XMLFormat<Integer>() {
        
        @Override
        protected Integer newInstance(final java.lang.Class<Integer> clazz, final InputElement in) throws XMLStreamException {
            return Integer.valueOf( in.getAttribute( "v" ) );
        };
        
        @Override
        public void read( final InputElement xml, final Integer obj ) throws XMLStreamException {
            // nothing to do
        }
        
        @Override
        public void write( final Integer obj, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "v", obj.toString() );
        }
        
    };

    public static final XMLFormat<Long> XML_LONG = new XMLFormat<Long>() {
        
        @Override
        protected Long newInstance(final java.lang.Class<Long> clazz, final InputElement in) throws XMLStreamException {
            return Long.valueOf( in.getAttribute( "v" ) );
        };
        
        @Override
        public void read( final InputElement xml, final Long obj ) throws XMLStreamException {
            // nothing to do
        }
        
        @Override
        public void write( final Long obj, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "v", obj.toString() );
        }
        
    };

    public static final XMLFormat<Float> XML_FLOAT = new XMLFormat<Float>() {
        
        @Override
        protected Float newInstance(final java.lang.Class<Float> clazz, final InputElement in) throws XMLStreamException {
            return Float.valueOf( in.getAttribute( "v" ) );
        };
        
        @Override
        public void read( final InputElement xml, final Float obj ) throws XMLStreamException {
            // nothing to do
        }
        
        @Override
        public void write( final Float obj, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "v", obj.toString() );
        }
        
    };

    public static final XMLFormat<Double> XML_DOUBLE = new XMLFormat<Double>() {
        
        @Override
        protected Double newInstance(final java.lang.Class<Double> clazz, final InputElement in) throws XMLStreamException {
            return Double.valueOf( in.getAttribute( "v" ) );
        };
        
        @Override
        public void read( final InputElement xml, final Double obj ) throws XMLStreamException {
            // nothing to do
        }
        
        @Override
        public void write( final Double obj, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "v", obj.toString() );
        }
        
    };

    public static final XMLFormat<Character> XML_CHARACTER = new XMLFormat<Character>() {
        
        @Override
        protected Character newInstance(final java.lang.Class<Character> clazz, final InputElement in) throws XMLStreamException {
            final String result = in.getAttribute( "v" );
            if ( result.length() > 1 ) {
                throw new XMLStreamException( "The attribute 'v' of type Character has illegal value (length > 1): " + result );
            }
            return Character.valueOf( result.charAt( 0 ) );
        };
        
        @Override
        public void read( final InputElement xml, final Character obj ) throws XMLStreamException {
            // nothing to do
        }
        
        @Override
        public void write( final Character obj, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "v", obj.toString() );
        }
        
    };

    public static final XMLFormat<Byte> XML_BYTE = new XMLFormat<Byte>() {
        
        @Override
        protected Byte newInstance(final java.lang.Class<Byte> clazz, final InputElement in) throws XMLStreamException {
            return Byte.valueOf( in.getAttribute( "v" ) );
        };
        
        @Override
        public void read( final InputElement xml, final Byte obj ) throws XMLStreamException {
            // nothing to do
        }
        
        @Override
        public void write( final Byte obj, final OutputElement output ) throws XMLStreamException {
            output.setAttribute( "v", obj.toString() );
        }
        
    };

    /**
     * An {@link XMLFormat} for {@link Calendar} that serialized those calendar
     * fields that contain actual data (these fields also are used by
     * {@link Calendar#equals(Object)}.
     */
    private static class XMLCalendarFormat extends XMLFormat<Calendar> {

        private final Field _zoneField;

        public XMLCalendarFormat() {
            try {
                _zoneField = Calendar.class.getDeclaredField( "zone" );
                _zoneField.setAccessible( true );
            } catch ( final Exception e ) {
                throw new RuntimeException( e );
            }
        }

        @Override
        public Calendar newInstance( final Class<Calendar> clazz, final InputElement arg1 ) throws XMLStreamException {
            if ( clazz.equals( GregorianCalendar.class ) ) {
                return GregorianCalendar.getInstance();
            }
            throw new IllegalArgumentException( "Calendar of type " + clazz.getName()
                    + " not yet supported. Please submit an issue so that it will be implemented." );
        }

        @Override
        public void read( final InputElement xml, final Calendar obj ) throws XMLStreamException {
            /* check if we actually need to set the timezone, as
             * TimeZone.getTimeZone is synchronized, so we might prevent this
             */
            final String timeZoneId = xml.getAttribute( "tz", "" );
            if ( !getTimeZone( obj ).getID().equals( timeZoneId ) ) {
                obj.setTimeZone( TimeZone.getTimeZone( timeZoneId ) );
            }
            obj.setMinimalDaysInFirstWeek( xml.getAttribute( "minimalDaysInFirstWeek", -1 ) );
            obj.setFirstDayOfWeek( xml.getAttribute( "firstDayOfWeek", -1 ) );
            obj.setLenient( xml.getAttribute( "lenient", true ) );
            obj.setTimeInMillis( xml.getAttribute( "timeInMillis", -1L ) );
        }

        @Override
        public void write( final Calendar obj, final OutputElement xml ) throws XMLStreamException {

            if ( !obj.getClass().equals( GregorianCalendar.class ) ) {
                throw new IllegalArgumentException( "Calendar of type " + obj.getClass().getName()
                        + " not yet supported. Please submit an issue so that it will be implemented." );
            }

            xml.setAttribute( "timeInMillis", obj.getTimeInMillis() );
            xml.setAttribute( "lenient", obj.isLenient() );
            xml.setAttribute( "firstDayOfWeek", obj.getFirstDayOfWeek() );
            xml.setAttribute( "minimalDaysInFirstWeek", obj.getMinimalDaysInFirstWeek() );
            xml.setAttribute( "tz", getTimeZone( obj ).getID() );
        }

        private TimeZone getTimeZone( final Calendar obj ) throws XMLStreamException {
            /* access the timezone via the field, to prevent cloning of the tz */
            try {
                return (TimeZone) _zoneField.get( obj );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

    };
    
    static class XMLEnumFormat extends XMLFormat<Enum<?>> {
        
        /**
         * {@inheritDoc}
         */
        @Override
        public Enum<?> newInstance( final Class<Enum<?>> clazz, final InputElement xml ) throws XMLStreamException {
            final String value = xml.getAttribute( "value", (String)null );
            final String clazzName = xml.getAttribute( "type", (String)null );
            try {
                @SuppressWarnings( "unchecked" )
                final Enum<?> enumValue = Enum.valueOf( Class.forName( clazzName ).asSubclass( Enum.class ), value );
                return enumValue;
            } catch ( final ClassNotFoundException e ) {
                throw new XMLStreamException( e );
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void read( final InputElement xml, final Enum<?> object ) throws XMLStreamException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( final Enum<?> object, final OutputElement xml ) throws XMLStreamException {
            xml.setAttribute( "value", object.name() );
            xml.setAttribute( "type", object.getClass().getName() );
        }
        
    }
    
    public static class XMLArrayFormat extends XMLFormat<Object> {
        
        public XMLArrayFormat() {
        }
        
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings( "unchecked" )
        @Override
        public Object newInstance( final Class clazz, final InputElement input ) throws XMLStreamException {
            System.out.println("XMLArrayFormat.newinstance invoked");
            try {
                final String componentType = input.getAttribute( "componentType", (String)null );
                final int length = input.getAttribute( "length", 0 );
                return Array.newInstance( Class.forName( componentType ) , length );
            } catch ( final Exception e ) {
                _log.log( Level.SEVERE, "caught exception", e );
                throw new XMLStreamException( e );
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void read( final InputElement input, final Object obj ) throws XMLStreamException {

            final Object[] arr = (Object[]) obj;
            int i = 0;
            while ( input.hasNext() ) {
                arr[i++] = input.getNext();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( final Object obj, final OutputElement output ) throws XMLStreamException {
            //System.out.println( "-- have array...");
            final Object[] array = (Object[]) obj;
            output.setAttribute( "type", "array" );
            output.setAttribute( "componentType", obj.getClass().getComponentType().getName() );
            output.setAttribute( "length", array.length );
            for( final Object item : array ) {
                output.getStreamWriter().writeStartElement( "i" );
                output.add( item );
                output.getStreamWriter().writeEndElement();
            }
        }
        
    }

    public static final XMLFormat<Collection<Object>> XMLCollectionFormat = new XMLFormat<Collection<Object>>() {
        
        @Override
        public void read( final InputElement xml, final Collection<Object> obj ) throws XMLStreamException {
            while ( xml.hasNext() ) {
                obj.add( xml.getNext() );
            }
        }
        
        @Override
        public void write( final Collection<Object> obj, final OutputElement output ) throws XMLStreamException {
            for( final Object item : obj ) {
                output.getStreamWriter().writeStartElement( "i" );
                output.add( item );
                output.getStreamWriter().writeEndElement();
            }
        }
        
    };

    public static final XMLFormat<Map<Object, Object>> XMLMapFormat = new XMLFormat<Map<Object, Object>>() {
        
        @Override
        public void read( final InputElement xml, final Map<Object, Object> obj ) throws XMLStreamException {
            while ( xml.hasNext() ) {
                final Object key = xml.get( "k" );
                final Object value = xml.get( "v" );
                obj.put( key, value );
            }
        }
        
        @Override
        public void write( final Map<Object, Object> obj, final OutputElement output ) throws XMLStreamException {
            for( final Map.Entry<Object, Object> entry : obj.entrySet() ) {
                //output.getStreamWriter().writeStartElement( "el" );
                output.add( entry.getKey(), "k" );
                output.add( entry.getValue(), "v" );
                //output.getStreamWriter().writeEndElement();
            }
        }
        
    };
    
}