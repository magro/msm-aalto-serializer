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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.stream.XMLStreamException;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.session.StandardSession;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.mutable.MutableInt;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import de.javakaffee.web.msm.MemcachedBackupSessionManager;
import de.javakaffee.web.msm.MemcachedBackupSessionManager.MemcachedBackupSession;
import de.javakaffee.web.msm.serializer.javolution.AaltoTranscoderTest.Person.Gender;
import de.javakaffee.web.msm.serializer.javolution.TestClasses.Container;
import de.javakaffee.web.msm.serializer.javolution.XMLBinding.XMLReader;
import de.javakaffee.web.msm.serializer.javolution.XMLBinding.XMLWriter;

/**
 * Test for {@link XStreamTranscoder}.
 * 
 * @author Martin Grotzke (martin.grotzke@freiheit.com) (initial creation)
 */
public class AaltoTranscoderTest extends MockObjectTestCase {

    private MemcachedBackupSessionManager _manager;
    private AaltoTranscoder _transcoder;

    @BeforeTest
    protected void beforeTest() {
        _manager = new MemcachedBackupSessionManager();

        final StandardContext container = new StandardContext();
        _manager.setContainer( container );

        final Mock webappLoaderControl = mock( WebappLoader.class );
        final WebappLoader webappLoader = (WebappLoader) webappLoaderControl.proxy();
        webappLoaderControl.expects( once() ).method( "setContainer" ).withAnyArguments();
        webappLoaderControl.expects( atLeastOnce() ).method( "getClassLoader" ).will(
                returnValue( Thread.currentThread().getContextClassLoader() ) );
        Assert.assertNotNull( webappLoader.getClassLoader(), "Webapp Classloader is null." );
        _manager.getContainer().setLoader( webappLoader );

        Assert.assertNotNull( _manager.getContainer().getLoader().getClassLoader(), "Classloader is null." );

        _transcoder = new AaltoTranscoder( _manager );
    }
    
    @Test(enabled=false)
    public void testSimple() throws Exception {
        final Person person = createPerson( "foo bar", Gender.MALE, "foo.bar@example.com" );
        final byte[] serialized = serialize( person );
        System.out.println( new String( serialized ) );
        assertDeepEquals( deserialize( serialized ), person );
    }
    
    @Test(enabled=false)
    public void testInnerClass() throws Exception {
        final Container container = TestClasses.createContainer( "some content" );
        final byte[] serialized = serialize( container );
        System.out.println( new String( serialized ) );
        assertDeepEquals( deserialize( serialized ), container );
    }
    
    private static class PrivateClass {
        private PrivateClass() {
        }
    }

    @DataProvider( name = "typesAsSessionAttributesProvider" )
    protected Object[][] createTypesAsSessionAttributesData() {
        return new Object[][] {
                { int.class, 42 },
                { long.class, 42 },
                { Boolean.class, Boolean.TRUE },
                { String.class, "42" },
                { Class.class, String.class },
                { Long.class, new Long( 42 ) },
                { Integer.class, new Integer( 42 ) },
                { Character.class, new Character( 'c' ) },
                { Byte.class, new Byte( "b".getBytes()[0] ) },
                { Double.class, new Double( 42d ) },
                { Float.class, new Float( 42f ) },
                { Short.class, new Short( (short) 42 ) },
                { BigDecimal.class, new BigDecimal( 42 ) },
                { AtomicInteger.class, new AtomicInteger( 42 ) },
                { AtomicLong.class, new AtomicLong( 42 ) },
                { MutableInt.class, new MutableInt( 42 ) },
                { Integer[].class, new Integer[] { 42 } },
                { Date.class, new Date( System.currentTimeMillis() - 10000 ) },
                { Calendar.class, Calendar.getInstance() },
                { ArrayList.class, new ArrayList<String>( Arrays.asList( "foo" ) ) },
                { int[].class, new int[] { 1, 2 } },
                { long[].class, new long[] { 1, 2 } },
                { short[].class, new short[] { 1, 2 } },
                { float[].class, new float[] { 1, 2 } },
                { double[].class, new double[] { 1, 2 } },
                { int[].class, new int[] { 1, 2 } },
                { byte[].class, "42".getBytes() },
                { char[].class, "42".toCharArray() },
                { String[].class, new String[] { "23", "42" } },
                { Person[].class, new Person[] { createPerson( "foo bar", Gender.MALE, 42 ) } }
                };
    }

    @Test( enabled = true, dataProvider = "typesAsSessionAttributesProvider" )
    public <T> void testTypesAsSessionAttributes( final Class<T> type, final T instance ) throws Exception {

        final MemcachedBackupSession session = _manager.createEmptySession();
        session.setValid( true );
        session.setAttribute( type.getSimpleName(), instance );

        //        System.out.println(new String(_transcoder.serialize( session )));
        assertDeepEquals( _transcoder.deserialize( _transcoder.serialize( session ) ), session );
    }
    
    @Test(enabled=false)
    void testInstantiate() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        final Object instance = Class.forName( "de.javakaffee.web.msm.MemcachedBackupSessionManager$MemcachedBackupSession" ).newInstance();
        System.out.println("got instance: " + instance);
        
        System.out.println( ConcurrentHashMap.class.isAssignableFrom( Map.class ) );
        System.out.println( Map.class.isAssignableFrom( ConcurrentHashMap.class ) );
        System.out.println( Integer.class.isAssignableFrom( Number.class ) );
        System.out.println( Number.class.isAssignableFrom( Integer.class ) );
        System.out.println( String[].class.isArray() );
        
        final String[] foo = new String[] { "foo" };
        System.out.println( foo.getClass().getComponentType() );
        
        System.out.println( ((String[]) Array.newInstance( Class.forName( "java.lang.String" ), 0 )).length );
        
        System.out.println( Class.forName( "java.util.Arrays$ArrayList" ));
        System.out.println( Class.forName( "de.javakaffee.web.msm.serializer.xstream.JavolutionTranscoderTest$Person" ));
        
    }
    
    public static class Foo {
        private final String[] _bars;
        private final List<String> _foos;
        private final Map<String,Integer> _bazens;
        public Foo() {
            _bars = new String[] { "foo", "bar" };
            _foos = new ArrayList<String>(Arrays.asList( "foo", "bar" ));
            _bazens = new HashMap<String, Integer>();
            _bazens.put( "foo", 1 );
            _bazens.put( "bar", 2 );
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode( _bars );
            result = prime * result + ( ( _bazens == null )
                ? 0
                : _bazens.hashCode() );
            result = prime * result + ( ( _foos == null )
                ? 0
                : _foos.hashCode() );
            return result;
        }
        @Override
        public boolean equals( final Object obj ) {
            if ( this == obj )
                return true;
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            final Foo other = (Foo) obj;
            if ( !Arrays.equals( _bars, other._bars ) )
                return false;
            if ( _bazens == null ) {
                if ( other._bazens != null )
                    return false;
            } else if ( !_bazens.equals( other._bazens ) )
                return false;
            if ( _foos == null ) {
                if ( other._foos != null )
                    return false;
            } else if ( !_foos.equals( other._foos ) )
                return false;
            return true;
        }
    }
    
    @Test(enabled=false)
    public void testArrays() throws Exception {
        final MemcachedBackupSessionManager manager = new MemcachedBackupSessionManager();
        manager.setContainer( new StandardContext() );
        final AaltoTranscoder transcoder = new AaltoTranscoder( manager );
        final Foo foo = new Foo();
        assertDeepEquals( transcoder.deserialize( transcoder.serialize( foo ) ), foo );
        
    }

    @Test(enabled=false)
    public void testCyclicDependencies() throws Exception {
        final MemcachedBackupSessionManager manager = new MemcachedBackupSessionManager();
        manager.setContainer( new StandardContext() );
        final AaltoTranscoder transcoder = new AaltoTranscoder( manager );

        final StandardSession session = manager.createEmptySession();
        session.setValid( true );
        session.setCreationTime( System.currentTimeMillis() );
        getField( StandardSession.class, "lastAccessedTime" ).set( session, System.currentTimeMillis() + 100 );
        session.setMaxInactiveInterval( 600 );

        final Person p1 = createPerson( "foo bar", Gender.MALE, "foo.bar@example.org", "foo.bar@example.com" );
        final Person p2 = createPerson( "bar baz", Gender.FEMALE, "bar.baz@example.org", "bar.baz@example.com" );
        p1.addFriend( p2 );
        p2.addFriend( p1 );
        
        session.setAttribute( "person1", p1 );
        session.setAttribute( "person2", p2 );
        
        final byte[] bytes = transcoder.serialize( session );
        // System.out.println( "xml: " + new String( bytes ) );
        assertDeepEquals( session, transcoder.deserialize( bytes ) );
        
        
    }

    @Test(enabled=false)
    public void testReadValueIntoObject() throws Exception {
        final MemcachedBackupSessionManager manager = new MemcachedBackupSessionManager();
        manager.setContainer( new StandardContext() );
        final AaltoTranscoder transcoder = new AaltoTranscoder( manager );

        final StandardSession session = manager.createEmptySession();
        session.setValid( true );
        session.setCreationTime( System.currentTimeMillis() );
        getField( StandardSession.class, "lastAccessedTime" ).set( session, System.currentTimeMillis() + 100 );
        session.setMaxInactiveInterval( 600 );

        session.setId( "foo" );

        session.setAttribute( "person1", createPerson( "foo bar", Gender.MALE, "foo.bar@example.org", "foo.bar@example.com" ) );
        session.setAttribute( "person2", createPerson( "bar baz", Gender.FEMALE, "bar.baz@example.org", "bar.baz@example.com" ) );

        final long start1 = System.nanoTime();
        transcoder.serialize( session );
        System.out.println("xstream-ser took " + (System.nanoTime() - start1)/1000);

        final long start2 = System.nanoTime();
        transcoder.serialize( session );
        System.out.println("xstream-ser took " + (System.nanoTime() - start2)/1000);
        
        final long start3 = System.nanoTime();
        final byte[] json = transcoder.serialize( session );
        final StandardSession readJSONValue = (StandardSession) transcoder.deserialize( json );
        System.out.println("xstream-round took " + (System.nanoTime() - start3)/1000);

        System.out.println( "Have json: " + readJSONValue.getId() );
        assertDeepEquals( readJSONValue, session );

        final long start4 = System.nanoTime();
        final StandardSession readJavaValue = javaRoundtrip( session, manager );
        System.out.println("java-round took " + (System.nanoTime() - start4)/1000);
        assertDeepEquals( readJavaValue, session );

        assertDeepEquals( readJSONValue, readJavaValue );

        System.out.println( ToStringBuilder.reflectionToString( session ) );
        System.out.println( ToStringBuilder.reflectionToString( readJSONValue ) );
        System.out.println( ToStringBuilder.reflectionToString( readJavaValue ) );

    }

    private Person createPerson( final String name, final Gender gender, final Integer age, final String... emailAddresses ) {
        final Person person = new Person();
        person.setName( name );
        person.setGender( gender );
        person.setAge( age );
        if ( emailAddresses != null ) {
            final HashMap<String, Object> props = new HashMap<String, Object>();
            for ( int i = 0; i < emailAddresses.length; i++ ) {
                final String emailAddress = emailAddresses[i];
                props.put( "email" + i, new Email( name, emailAddress ) );
            }
            person.setProps( props );
        }
        return person;
    }

    private Person createPerson( final String name, final Gender gender, final String... emailAddresses ) {
        return createPerson( name, gender, null, emailAddresses );
    }

    private Field getField( final Class<?> clazz, final String name ) throws NoSuchFieldException {
        final Field field = clazz.getDeclaredField( name );
        field.setAccessible( true );
        return field;
    }
    
    /*
     * person2=Person
     * [_gender=FEMALE, _name=bar baz,
     *      _props={email0=Email [_email=bar.baz@example.org, _name=bar baz],
     *          email1=Email [_email=bar.baz@example.com, _name=bar baz]}],
     * person1=Person [_gender=MALE, _name=foo bar,
     *      _props={email0=Email [_email=foo.bar@example.org, _name=foo bar],
     *          email1=Email [_email=foo.bar@example.com, _name=foo bar]}]}
     *          
     * but was:
     * person2={name=bar baz,
     *      props={email0={name=bar baz, email=bar.baz@example.org},
     *          email1={name=bar baz, email=bar.baz@example.com}}, gender=FEMALE}
     * person1={name=foo bar,
     *      props={email0={name=foo bar, email=foo.bar@example.org},
     *          email1={name=foo bar, email=foo.bar@example.com}}, gender=MALE}}
     */

    private void assertDeepEquals( final Object one, final Object another ) throws Exception {
        if ( one == another ) {
            return;
        }
        if ( one == null && another != null || one != null && another == null ) {
            Assert.fail( "One of both is null: " + one + ", " + another );
        }
        
        Assert.assertEquals( one.getClass(), another.getClass() );
        if ( one.getClass().isPrimitive() || one instanceof String || one instanceof Character || one instanceof Boolean ) {
            Assert.assertEquals( one, another );
            return;
        }

        if ( Map.class.isAssignableFrom( one.getClass() ) ) {
            final Map<?, ?> m1 = (Map<?, ?>) one;
            final Map<?, ?> m2 = (Map<?, ?>) another;
            Assert.assertEquals( m1.size(), m2.size() );
            for ( final Map.Entry<?, ?> entry : m1.entrySet() ) {
                assertDeepEquals( entry.getValue(), m2.get( entry.getKey() ) );
            }
            return;
        }

        if ( Number.class.isAssignableFrom( one.getClass() ) ) {
            Assert.assertEquals( ( (Number) one ).longValue(), ( (Number) another ).longValue() );
            return;
        }

        Class<? extends Object> clazz = one.getClass();
        while ( clazz != null ) {
            assertEqualDeclaredFields( clazz, one, another );
            clazz = clazz.getSuperclass();
        }

    }

    private void assertEqualDeclaredFields( final Class<? extends Object> clazz, final Object one, final Object another )
        throws Exception, IllegalAccessException {
        for ( final Field field : clazz.getDeclaredFields() ) {
            field.setAccessible( true );
            if ( !Modifier.isTransient( field.getModifiers() ) ) {
                assertDeepEquals( field.get( one ), field.get( another ) );
            }
        }
    }

    private StandardSession javaRoundtrip( final StandardSession session, final MemcachedBackupSessionManager manager )
        throws IOException, ClassNotFoundException {

        final long start1 = System.nanoTime();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream( bos );
        session.writeObjectData( oos );
        oos.close();
        bos.close();
        System.out.println("java-ser took " + (System.nanoTime() - start1)/1000);

        final ByteArrayInputStream bis = new ByteArrayInputStream( bos.toByteArray() );
        final ObjectInputStream ois = new ObjectInputStream( bis );
        final StandardSession readSession = manager.createEmptySession();
        readSession.readObjectData( ois );
        ois.close();
        bis.close();

        return readSession;
    }

    public static class Person implements Serializable {

        private static final long serialVersionUID = 1L;

        static enum Gender {
                MALE,
                FEMALE
        }

        private String _name;
        private Gender _gender;
        private Integer _age;
        private Map<String, Object> _props;
        private final Collection<Person> _friends = new ArrayList<Person>();

        public String getName() {
            return _name;
        }
        
        public void addFriend( final Person p ) {
            _friends.add( p );
        }

        public void setName( final String name ) {
            _name = name;
        }

        public Map<String, Object> getProps() {
            return _props;
        }

        public void setProps( final Map<String, Object> props ) {
            _props = props;
        }

        public Gender getGender() {
            return _gender;
        }

        public void setGender( final Gender gender ) {
            _gender = gender;
        }

        public Collection<Person> getFriends() {
            return _friends;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( _friends == null )
                ? 0
                : _friends.hashCode() );
            result = prime * result + ( ( _gender == null )
                ? 0
                : _gender.hashCode() );
            result = prime * result + ( ( _age == null )
                    ? 0
                    : _age.hashCode() );
            result = prime * result + ( ( _name == null )
                ? 0
                : _name.hashCode() );
            result = prime * result + ( ( _props == null )
                ? 0
                : _props.hashCode() );
            return result;
        }

        @Override
        public boolean equals( final Object obj ) {
            if ( this == obj )
                return true;
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            final Person other = (Person) obj;
            if ( _friends == null ) {
                if ( other._friends != null )
                    return false;
            }
            else if ( !flatEquals( _friends, other._friends ) )
                return false;
            /*else if ( !_friends.equals( other._friends ) )
                return false;
                */
            if ( _gender == null ) {
                if ( other._gender != null )
                    return false;
            } else if ( !_gender.equals( other._gender ) )
                return false;
            if ( _age == null ) {
                if ( other._age != null )
                    return false;
            } else if ( !_age.equals( other._age ) )
                return false;
            if ( _name == null ) {
                if ( other._name != null )
                    return false;
            } else if ( !_name.equals( other._name ) )
                return false;
            if ( _props == null ) {
                if ( other._props != null )
                    return false;
            } else if ( !_props.equals( other._props ) )
                return false;
            return true;
        }

        /**
         * @param friends
         * @param friends2
         * @return
         */
        private boolean flatEquals( final Collection<?> c1, final Collection<?> c2 ) {
            return c1 == c2 || c1 != null && c2 != null && c1.size() == c2.size();
        }

        @Override
        public String toString() {
            return "Person [_friends.size=" + (_friends == null ? "<null>" : _friends.size()) + ", _gender=" + _gender + ", _name=" + _name + ", _props=" + _props + "]";
        }

        public Integer getAge() {
            return _age;
        }

        public void setAge( final Integer age ) {
            _age = age;
        }

    }

    public static class Email implements Serializable {

        private static final long serialVersionUID = 1L;

        private String _name;
        private String _email;
        
        public Email() {
        }

        public Email( final String name, final String email ) {
            super();
            _name = name;
            _email = email;
        }

        public String getName() {
            return _name;
        }

        public void setName( final String name ) {
            _name = name;
        }

        public String getEmail() {
            return _email;
        }

        public void setEmail( final String email ) {
            _email = email;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( _email == null )
                ? 0
                : _email.hashCode() );
            result = prime * result + ( ( _name == null )
                ? 0
                : _name.hashCode() );
            return result;
        }

        @Override
        public boolean equals( final Object obj ) {
            if ( this == obj )
                return true;
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            final Email other = (Email) obj;
            if ( _email == null ) {
                if ( other._email != null )
                    return false;
            } else if ( !_email.equals( other._email ) )
                return false;
            if ( _name == null ) {
                if ( other._name != null )
                    return false;
            } else if ( !_name.equals( other._name ) )
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Email [_email=" + _email + ", _name=" + _name + "]";
        }

    }

    protected byte[] serialize( final Object o ) {
        if ( o == null ) {
            throw new NullPointerException( "Can't serialize null" );
        }
        
        XMLWriter writer = null;
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            writer = new XMLBinding().newXMLWriter(bos);
            writer.write( o );
            writer.flush();
            //getLogger().info( "Returning deserialized:\n" + new String(bos.toByteArray()) );
            return bos.toByteArray();
        } catch ( final Exception e ) {
            throw new IllegalArgumentException( "Non-serializable object", e );
        } finally {
            writer.close();
        }

    }
    
    protected Object deserialize( final byte[] in ) {
        XMLReader reader = null;
        try {
            final ByteArrayInputStream bis = new ByteArrayInputStream( in );
            reader = new XMLBinding().newXMLReader( bis );
            return reader.read();
        } catch ( final XMLStreamException e ) {
            throw new RuntimeException( e );
        } finally {
            reader.close();
        }
    }

}
