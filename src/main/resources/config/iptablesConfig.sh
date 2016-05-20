# FLUSH RULES
iptables -P INPUT ACCEPT
iptables -P OUTPUT ACCEPT
iptables -P FORWARD ACCEPT
iptables -F
iptables -X

# ALLOW LOCALHOST
iptables -A INPUT -t filter -s 127.0.0.1 -j ACCEPT

# FLUSH RULES
iptables -P INPUT ACCEPT
iptables -P OUTPUT ACCEPT
iptables -P FORWARD ACCEPT
iptables -F
iptables -X

# ALLOW LOCALHOST
iptables -A INPUT -t filter -s 127.0.0.1 -j ACCEPT

# SERVICES
# SSH
iptables -A INPUT -p tcp --dport 22 -j ACCEPT
# LOGIN
iptables -A INPUT -p tcp --dport 2106 -j ACCEPT
# INTERNAL
iptables -A INPUT -p tcp --dport 9014 -j ACCEPT
# GAME
# iptables -A INPUT -p tcp --dport 7777 -j ACCEPT
# MYSQL
iptables -A INPUT -p tcp --dport 3306 -j ACCEPT


# BLOCK THE DEVIL
iptables -A INPUT -p tcp -m tcp --tcp-flags FIN,SYN,RST,PSH,ACK,URG NONE -j DROP
iptables -A INPUT -p tcp -m tcp --tcp-flags FIN,SYN FIN,SYN -j DROP
iptables -A INPUT -p tcp -m tcp --tcp-flags SYN,RST SYN,RST -j DROP
iptables -A INPUT -p tcp -m tcp --tcp-flags FIN,RST FIN,RST -j DROP
iptables -A INPUT -p tcp -m tcp --tcp-flags FIN,ACK FIN -j DROP
iptables -A INPUT -p tcp -m tcp --tcp-flags ACK,URG URG -j DROP
iptables -A INPUT -p tcp -m tcp --tcp-flags PSH,ACK PSH -j DROP

# ALLOW NEW CONNECTIONS (JUST IN CASE)
iptables -A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT

# DROP USELESS UDP/ TCP
iptables -A INPUT -p udp -j DROP
iptables -A INPUT -p tcp --syn -j DROP

# BLOCK ICMP (PING/ TRACEROUTE ETC)
#iptables -A INPUT -p icmp -j DROP
#iptables -A INPUT -p icmp -s 78.56.128.136 -d 127.0.0.1 -j ACCEPT

# LIST RULES
iptables -L -v