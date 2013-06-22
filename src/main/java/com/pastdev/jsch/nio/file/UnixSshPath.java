package com.pastdev.jsch.nio.file;


import static com.pastdev.jsch.nio.file.UnixSshFileSystemProvider.PATH_SEPARATOR;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class UnixSshPath extends AbstractSshPath {
    private boolean absolute;
    private String[] parts;

    UnixSshPath( UnixSshFileSystem unixSshFileSystem, String path ) {
        super( unixSshFileSystem );

        // normalize path string and discover separator indexes.
        // could probably optimize this at some point...
        if ( !path.isEmpty() ) {
            String[] parts = path.split( PATH_SEPARATOR + "+", 0 );
            if ( parts[0].isEmpty() ) {
                this.absolute = true;
                this.parts = Arrays.copyOfRange( parts, 1, parts.length - 1 );
                int newLength = parts.length - 1;
                this.parts = new String[newLength];
                System.arraycopy( parts, 1, this.parts, 0, newLength );
            }
            else {
                this.parts = parts;
            }
        }
    }

    private UnixSshPath( UnixSshFileSystem unixSshFileSystem, boolean isAbsolute, String... parts ) {
        super( unixSshFileSystem );
        this.absolute = isAbsolute;
        this.parts = parts == null ? new String[0] : parts;
    }

    @Override
    public int compareTo( Path o ) {
        if ( !getFileSystem().provider().equals( o.getFileSystem().provider() ) ) {
            throw new ClassCastException( "cannot compare paths from 2 different provider instances" );
        }
        return toString().compareTo( ((UnixSshPath)o).toString() );
    }

    @Override
    public boolean endsWith( Path path ) {
        if ( !getFileSystem().equals( path.getFileSystem() ) ) {
            return false;
        }
        if ( path.isAbsolute() && !isAbsolute() ) {
            return false;
        }

        int count = getNameCount();
        int otherCount = path.getNameCount();
        if ( otherCount > count ) {
            return false;
        }

        for ( count--, otherCount--; otherCount >= 0; count--, otherCount-- ) {
            if ( !path.getName( otherCount ).toString().equals( getName( count ).toString() ) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean endsWith( String path ) {
        return endsWith( new UnixSshPath( getFileSystem(), path ) );
    }

    @Override
    public UnixSshPath getFileName() {
        if ( parts.length == 0 ) return null;
        return new UnixSshPath( getFileSystem(), false, getFileNameString() );
    }

    String getFileNameString() {
        return parts[parts.length - 1];
    }

    @Override
    public UnixSshFileSystem getFileSystem() {
        return (UnixSshFileSystem)super.getFileSystem();
    }

    @Override
    public String getHostname() {
        return ((UnixSshFileSystem)getFileSystem()).getUri().getHost();
    }

    @Override
    public UnixSshPath getName( int index ) {
        if ( index < 0 ) {
            throw new IllegalArgumentException();
        }
        if ( index >= parts.length ) {
            throw new IllegalArgumentException();
        }

        return new UnixSshPath( (UnixSshFileSystem)getFileSystem(),
                false, parts[index] );
    }

    @Override
    public int getNameCount() {
        return parts.length;
    }

    @Override
    public UnixSshPath getParent() {
        if ( parts.length == 0 && !isAbsolute() ) {
            return null;
        }
        if ( parts.length <= 1 ) {
            return new UnixSshPath( (UnixSshFileSystem)getFileSystem(), isAbsolute() );
        }
        return new UnixSshPath( (UnixSshFileSystem)getFileSystem(), isAbsolute(),
                Arrays.copyOfRange( parts, 0, parts.length - 1 ) );
    }

    @Override
    public int getPort() {
        return ((UnixSshFileSystem)getFileSystem()).getUri().getPort();
    }

    @Override
    public Path getRoot() {
        if ( isAbsolute() ) {
            return new UnixSshPath( (UnixSshFileSystem)getFileSystem(), true );
        }
        else {
            return null;
        }
    }

    @Override
    public String getUsername() {
        return ((UnixSshFileSystem)getFileSystem()).getUri().getUserInfo();
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public Iterator<Path> iterator() {
        return new Iterator<Path>() {
            int index = 0;
            int count = getNameCount();

            public boolean hasNext() {
                return index < count;
            }

            public Path next() {
                return getName( index++ );
            }

            public void remove() {
                // path is immutable... dont want to allow changes
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Path normalize() {
        List<String> partsList = new ArrayList<String>();
        for ( String part : parts ) {
            if ( part.equals( "." ) ) {
                continue;
            }
            else if ( part.equals( ".." ) ) {
                int size = partsList.size();
                if ( size > 0 ) {
                    partsList.remove( size - 1 );
                }
            }
            else {
                partsList.add( part );
            }
        }
        return new UnixSshPath( getFileSystem(), isAbsolute(),
                partsList.toArray( new String[partsList.size()] ) );
    }

    @Override
    public WatchKey register( WatchService arg0, Kind<?>... arg1 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WatchKey register( WatchService arg0, Kind<?>[] arg1, Modifier... arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path relativize( Path path ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path resolve( Path other ) {
        if ( other.isAbsolute() ) {
            return other;
        }
        else if ( other.getNameCount() == 0 ) {
            return this;
        }

        int count = other.getNameCount();
        String[] combined = new String[parts.length + count];
        System.arraycopy( parts, 0, combined, 0, parts.length );
        int index = parts.length;
        for ( Path otherPart : other ) {
            combined[index++] = otherPart.toString();
        }
        return new UnixSshPath( getFileSystem(), isAbsolute(), combined );
    }

    @Override
    public Path resolve( String other ) {
        return resolve( new UnixSshPath( getFileSystem(), other ) );
    }

    @Override
    public Path resolveSibling( Path other ) {
        return getParent().resolve( other );
    }

    @Override
    public Path resolveSibling( String other ) {
        return resolveSibling( new UnixSshPath( getFileSystem(), other ) );
    }

    @Override
    public boolean startsWith( Path other ) {
        if ( !getFileSystem().equals( other.getFileSystem() ) ) {
            return false;
        }
        if ( (other.isAbsolute() && !isAbsolute()) ||
                (isAbsolute() && !other.isAbsolute()) ) {
            return false;
        }

        int count = getNameCount();
        int otherCount = other.getNameCount();
        if ( otherCount > count ) {
            return false;
        }
        
        for ( int i = 0; i < otherCount; i++ ) {
            if ( !other.getName( i ).toString().equals( getName( i ).toString() ) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean startsWith( String other ) {
        return startsWith( new UnixSshPath( getFileSystem(), other ) );
    }

    @Override
    public Path subpath( int start, int end ) {
        String[] parts = new String[end-start];
        for ( int i = start; i < end; i++ ) {
            parts[i] = getName( i ).toString();
        }
        return new UnixSshPath( getFileSystem(), false, parts );
    }

    @Override
    public Path toAbsolutePath() {
        if ( isAbsolute() ) {
            return this;
        }
        else {
            UnixSshFileSystem fileSystem = (UnixSshFileSystem)getFileSystem();
            return fileSystem.getPath(
                    fileSystem.getDefaultDirectory() + PATH_SEPARATOR + toString() );
        }
    }

    @Override
    public File toFile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException( "this opens up a WHOLE new can of worms.  im just not ready for that" );
    }

    @Override
    public Path toRealPath( LinkOption... linkOptions ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for ( String part : parts ) {
            if ( builder.length() > 0 || isAbsolute() ) {
                builder.append( PATH_SEPARATOR );
            }
            builder.append( part );
        }

        return builder.toString();
    }

    @Override
    public URI toUri() {
        return ((UnixSshFileSystem)getFileSystem()).getUri().resolve( toString() );
    }
}