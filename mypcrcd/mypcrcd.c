#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <ctype.h>

#include <unistd.h>
#include <syslog.h>
#include <signal.h>
#include <sysexits.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <errno.h>

#include <X11/Xlib.h>
#include <X11/keysym.h>
#include <X11/XF86keysym.h>
#include <X11/extensions/XTest.h>


#define UNUSED( x ) ( (void)x )
#define STRLEN( s ) ( sizeof(s)-1 )

#define MYPCRC_PORT 10101
#define MYPCRC_BUFF 64
#define MYPCRC_PASS "a93abd1ee31d66e5e161c2a27e3f75a7"


static int run  =  0;
static int sock = -1;
static int fd   = -1;
static int auth =  0;


static void help( void );

static void x11_send_keystroke( int ctrl, int alt, int shift, KeySym key );

static void terminate( int sig );

static int server_init( void );
static void server_shutdown( void );
static void server_listen( void );

static void client_handle( void );
static void client_shutdown( void );
static void client_dump_bytes( char *data, int size );
static void client_parse( char *data, int size );


int main( int argc, char **argv )
{
	if( 2 == argc &&
	    (0 == strcmp("-h", argv[1]) ||
	     0 == strcmp("--help", argv[1])) ) {
	     help();
	     return EXIT_SUCCESS;
	}
	else
	if( 1 != argc ) {
		help();
		return EXIT_FAILURE;
	}

	daemon( 0, 0 );

	openlog( "mypcrc", LOG_NOWAIT | LOG_PID, LOG_USER );

	signal( SIGTERM, terminate );

	if( 0 != server_init() ) {
		closelog();
		return EXIT_FAILURE;
	}

	server_listen();

	client_shutdown();
	server_shutdown();

	closelog();

	return EXIT_SUCCESS;
}


static void help( void )
{
	printf(
		"mypcrc - a daemon for my pc remote control\n"
		"usage: mypcrc [-h | --help]\n"
		"options:\n"
		"  -h, --help  show this help\n"
	);
}

static void x11_send_keystroke( int ctrl, int alt, int shift, KeySym key )
{
	Display *d = XOpenDisplay( NULL );
	if( NULL == d ) {
		return;
	}

	Window w;
	int r;
	XGetInputFocus( d, &w, &r );

	char *n;
	if( 0 != XFetchName(d, w, &n) ) {
		if( STRLEN("VLC media player") > strlen(n) ||
		    0 != strcmp("VLC media player", n+(strlen(n)-STRLEN("VLC media player"))) ) {
			XFree( n );
			XCloseDisplay( d );
			return;
		}
		XFree( n );
	}
	else {
		XCloseDisplay( d );
		return;
	}

	if( 0 != ctrl ) {
		XTestFakeKeyEvent( d, XKeysymToKeycode(d, XK_Control_L), True, CurrentTime);
	}

	if( 0 != alt ) {
		XTestFakeKeyEvent( d, XKeysymToKeycode(d, XK_Alt_L), True, CurrentTime);
	}

	if( 0 != shift ) {
		XTestFakeKeyEvent( d, XKeysymToKeycode(d, XK_Shift_L), True, CurrentTime);
	}

	if( 0 != key ) {
		XTestFakeKeyEvent( d, XKeysymToKeycode(d, key), True, CurrentTime);
		XTestFakeKeyEvent( d, XKeysymToKeycode(d, key), False, CurrentTime);
	}

	if( 0 != alt ) {
		XTestFakeKeyEvent( d, XKeysymToKeycode(d, XK_Alt_L), False, CurrentTime);
	}

	if( 0 != shift ) {
		XTestFakeKeyEvent( d, XKeysymToKeycode(d, XK_Shift_L), False, CurrentTime);
	}

	if( 0 != ctrl ) {
		XTestFakeKeyEvent( d, XKeysymToKeycode(d, XK_Control_L), False, CurrentTime);
	}

	XCloseDisplay( d );
}

static void terminate( int sig )
{
	UNUSED( sig );

	static int force = 0;


	if( 0 != force ) {
		syslog( LOG_WARNING, "forcing daemon to shut down" );
		exit( EX_OK );
	}

	force = !0;

	client_shutdown();
	server_shutdown();
}

static int server_init( void )
{
	sock = socket( AF_INET, SOCK_STREAM, 0 );
	if( -1 == sock ) {
		syslog( LOG_ERR, "could not create socket: %m" );
		return !0;
	}

	int reuseaddr = 1;
	if( -1 == setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (void *)&reuseaddr, sizeof(reuseaddr)) ) {
		syslog( LOG_ERR, "could not set socket options: %m" );
		server_shutdown();
		return !0;
	}

	struct sockaddr_in saddr;
	saddr.sin_family = AF_INET;
	saddr.sin_addr.s_addr = INADDR_ANY;
	saddr.sin_port = htons( MYPCRC_PORT );
	if( -1 == bind(sock, (struct sockaddr *)&saddr, sizeof(saddr)) ) {
		syslog( LOG_ERR, "could not bind name to socket: %m" );
		server_shutdown();
		return !0;
	}

	if( -1 == listen(sock, 2) ) {
		syslog( LOG_ERR,"could not listen on socket: %m" );
		server_shutdown();
		return !0;
	}

	run = !0;

	return 0;
}

static void server_shutdown( void )
{
	if( -1 != sock ) {
		run = 0;
		shutdown( sock, SHUT_RDWR );
		sock = -1;
	}
}

static void server_listen( void )
{
	while( 0 != run ) {
		struct sockaddr_in saddr;
		size_t salen = sizeof(saddr);

		fd = accept( sock, (struct sockaddr *)&saddr, (socklen_t *)&salen );

		if( -1 == fd && EINTR != errno &&
		    (EBADF != errno && 0 != run) ) {
			syslog( LOG_ERR, "could not accept connection: %m" );
		}

		if( -1 == fd ) {
			continue;
		}

		client_handle();
	}
}

static void client_handle( void )
{
	char data[MYPCRC_BUFF+1];


	while( 0 != run &&
	       -1 != fd ) {
		ssize_t size = read( fd, data, sizeof(data)-1 );
		if( 0 == size ) {
			break;
		}
		else
		if( -1 == size ) {
			syslog( LOG_ERR, "could not read data from client: %m" );
			break;
		}

		data[size] = '\0';
#ifdef DEBUG
		client_dump_bytes( data, size );
#endif
		client_parse( data, size );
	}

	client_shutdown();
}

static void client_shutdown( void )
{
	if( -1 != fd ) {
		shutdown( fd, SHUT_RDWR );
		fd = -1;
		auth = 0;
	}
}

#ifdef DEBUG
static void client_dump_bytes( char *data, int size )
{
	if( NULL == data ) {
		syslog( LOG_DEBUG, "size: %d data: NULL", size );
		return;
	}
	else
	if( 0 == size ) {
		syslog( LOG_DEBUG, "size: 0 data: ?" );
		return;
	}

	char *ptr1;
	int count;
	for( count = 0, ptr1 = data;
	     data+size > ptr1;
	     ptr1++ ) {
		if( 0 == isprint(ptr1[0]) ||
		    0 != isspace(ptr1[0]) ||
		    '<' == ptr1[0] ||
		    '>' == ptr1[0] ) {
			count += 4;
		}
		else {
			count++;
		}
	}

	char *buf = malloc( count+1 );
	if( NULL == buf ) {
		syslog( LOG_ERR, "could not allocate %d bytes for buffer", count+1 );
		return;
	}

	char *ptr2;
	for( count = 0, ptr1 = data, ptr2 = buf;
	     data+size > ptr1;
	     ptr1++ ) {
		if( 0 == isprint(ptr1[0]) ||
		     0 != isspace(ptr1[0]) ||
		    '<' == ptr1[0] ||
		    '>' == ptr1[0] ) {
			ptr2[0] = '<';
			ptr2[1] = ( (unsigned char)ptr1[0] & 0xF0 ) >> 4;
			ptr2[1] += ptr2[1] < 10 ? '0' : '7';
			ptr2[2] = (unsigned char)ptr1[0] & 0x0F;
                        ptr2[2] += ptr2[2] < 10 ? '0' : '7';
			ptr2[3] = '>';
			ptr2 += 4;
		}
		else {
			ptr2[0] = ptr1[0];
			ptr2++;
		}
	}
	ptr2[0] = '\0';

	syslog( LOG_DEBUG, "size: %d data: %s", size, buf );

	free( buf );
}
#endif

static void client_parse( char *data, int size )
{
	UNUSED( size );


	if( 0 == strcmp("QUIT\r\n", data) ) {
		client_shutdown();
	}
	else
	if( 0 == strcmp("PING\r\n", data) ) {
		/* Do nothing. */
	}
	else
	if( 0 == strncmp("AUTH ", data, STRLEN("AUTH ")) ) {
		if( 0 != auth ) {
			return;
		}
		if( 0 == strncmp(MYPCRC_PASS, data+STRLEN("AUTH "), STRLEN(MYPCRC_PASS)) &&
		    0 == strcmp("\r\n", data+STRLEN("AUTH ")+STRLEN(MYPCRC_PASS)) ) {
		    auth = !0;
		}
		else {
			syslog( LOG_ERR, "client authentication failed" );
			client_shutdown();
			return;
		}
	}
	else
	if( 0 == strncmp("FUNC ", data, STRLEN("FUNC ")) ) {
		if( 0 == auth ) {
			syslog( LOG_ERR, "unauthenticated client sent function" );
			client_shutdown();
			return;
		}
		#define IS_FUNC( s ) ( 0 == strncmp(s, data+STRLEN("FUNC "), STRLEN(s)) )
		if( IS_FUNC("quit\r\n") ) {
			x11_send_keystroke( !0, 0, 0, XK_Q ); /* Ctrl+Q */
		}
		else
		if( IS_FUNC("stop\r\n") ) {
			x11_send_keystroke( 0, 0, 0, XK_S ); /* s */
		}
		else
		if( IS_FUNC("play-pause\r\n") ) {
			x11_send_keystroke( 0, 0, 0, XK_space ); /* space */
		}
		else
		if( IS_FUNC("play\r\n") ) {
			x11_send_keystroke( 0, 0, 0, XK_bracketright ); /* ] */
		}
		else
		if( IS_FUNC("pause\r\n") ) {
			x11_send_keystroke( 0, 0, 0, XK_bracketleft ); /* [ */
		}
		else
		if( IS_FUNC("toggle-fullscreen\r\n") ) {
			x11_send_keystroke( 0, 0, 0, XK_F ); /* f */
		}
		else
		if( IS_FUNC("leave-fullscreen\r\n") ) {
			x11_send_keystroke( 0, 0, 0, XK_Escape ); /* escape */
		}
		else
		if( IS_FUNC("vol-mute\r\n") ) {
			/* x11_send_keystroke( 0, 0, 0, XK_M ); */ /* m */
			x11_send_keystroke( 0, 0, 0, XF86XK_AudioMute );
		}
		else
		if( IS_FUNC("vol-up\r\n") ) {
			/* x11_send_keystroke( !0, 0, 0, XK_Up ); */ /* Ctrl+Up */
			x11_send_keystroke( 0, 0, 0, XF86XK_AudioRaiseVolume );
		}
		else
		if( IS_FUNC("vol-down\r\n") ) {
			/* x11_send_keystroke( !0, 0, 0, XK_Down ); */ /* Ctrl+Down */
			x11_send_keystroke( 0, 0, 0, XF86XK_AudioLowerVolume ); 
		}
		else
		if( IS_FUNC("jump-extrashort\r\n") ) {
			x11_send_keystroke( 0, 0, !0, XK_Left ); /* Shift+Left */
		}
		else
		if( IS_FUNC("jump+extrashort\r\n") ) {
			x11_send_keystroke( 0, 0, !0, XK_Right ); /* Shift+Right */
		}
		else
		if( IS_FUNC("jump-medium\r\n") ) {
			x11_send_keystroke( !0, 0, 0, XK_Left ); /* Ctrl+Left */
		}
		else
		if( IS_FUNC("jump+medium\r\n") ) {
			x11_send_keystroke( !0, 0, 0, XK_Right ); /* Ctrl+Right */
		}
		else {
			syslog( LOG_ERR, "unknown function from client" );
			client_shutdown();
			return;
		}
		#undef IS_FUNC
	}
	else {
		syslog( LOG_ERR, "unknown command from client" );
		client_shutdown();
	}
}
