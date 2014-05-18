all:
	gcc -Wall -Wextra -std=gnu99 -ggdb -omypcrcd mypcrcd.c -lX11 -lXtst -DNODEBUG

clean:
	rm -f mypcrcd

distclean: clean

run:
	./mypcrcd

stop:
	killall -15 mypcrcd

kill:
	killall -9 mypcrcd

show:
	@ps aux | grep mypcrcd | grep -v grep

tail:
	@tail -f /var/log/syslog

demo: all run show tail

commit:
	git commit -a --allow-empty-message

help:
	@echo Required packages:
	@echo libx11-dev
	@echo libxext-dev
