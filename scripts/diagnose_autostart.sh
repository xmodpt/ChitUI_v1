#!/bin/bash
# ChitUI Autostart Diagnostic Tool
# This script helps identify how ChitUI is auto-starting on your system

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   ChitUI Autostart Diagnostic Tool         ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════╝${NC}"
echo ""

echo -e "${YELLOW}Checking systemd services...${NC}"
echo "----------------------------------------"

# Check for chitui-plus service
if [ -f "/etc/systemd/system/chitui-plus.service" ]; then
    echo -e "${GREEN}✓ Found:${NC} /etc/systemd/system/chitui-plus.service"
    sudo systemctl status chitui-plus --no-pager || true
else
    echo -e "${RED}✗ Not found:${NC} /etc/systemd/system/chitui-plus.service"
fi

# Check for any other chitui services
echo ""
echo -e "${YELLOW}Searching for other ChitUI service names...${NC}"
SERVICES=$(find /etc/systemd -name "*chit*.service" 2>/dev/null)
if [ -n "$SERVICES" ]; then
    echo -e "${GREEN}Found services:${NC}"
    echo "$SERVICES"
    for service in $SERVICES; do
        SERVICE_NAME=$(basename "$service")
        echo ""
        echo -e "${BLUE}Status of $SERVICE_NAME:${NC}"
        sudo systemctl status "$SERVICE_NAME" --no-pager || true
    done
else
    echo -e "${YELLOW}No systemd services found with 'chit' in the name${NC}"
fi

# Check for user services
echo ""
echo -e "${YELLOW}Checking user systemd services...${NC}"
echo "----------------------------------------"
USER_SERVICES=$(find ~/.config/systemd/user -name "*chit*.service" 2>/dev/null || echo "")
if [ -n "$USER_SERVICES" ]; then
    echo -e "${GREEN}Found user services:${NC}"
    echo "$USER_SERVICES"
else
    echo -e "${YELLOW}No user systemd services found${NC}"
fi

# Check rc.local
echo ""
echo -e "${YELLOW}Checking /etc/rc.local...${NC}"
echo "----------------------------------------"
if [ -f "/etc/rc.local" ]; then
    echo -e "${BLUE}Contents of /etc/rc.local:${NC}"
    cat /etc/rc.local | grep -v "^#" | grep -v "^$"
    echo ""
    if grep -q -i chit /etc/rc.local; then
        echo -e "${GREEN}✓ ChitUI found in /etc/rc.local${NC}"
    else
        echo -e "${YELLOW}No ChitUI entries in /etc/rc.local${NC}"
    fi
else
    echo -e "${YELLOW}/etc/rc.local not found${NC}"
fi

# Check crontab (user)
echo ""
echo -e "${YELLOW}Checking user crontab...${NC}"
echo "----------------------------------------"
CRON_USER=$(crontab -l 2>/dev/null | grep -i chit || echo "")
if [ -n "$CRON_USER" ]; then
    echo -e "${GREEN}✓ Found ChitUI in user crontab:${NC}"
    echo "$CRON_USER"
else
    echo -e "${YELLOW}No ChitUI entries in user crontab${NC}"
fi

# Check crontab (root)
echo ""
echo -e "${YELLOW}Checking root crontab...${NC}"
echo "----------------------------------------"
CRON_ROOT=$(sudo crontab -l 2>/dev/null | grep -i chit || echo "")
if [ -n "$CRON_ROOT" ]; then
    echo -e "${GREEN}✓ Found ChitUI in root crontab:${NC}"
    echo "$CRON_ROOT"
else
    echo -e "${YELLOW}No ChitUI entries in root crontab${NC}"
fi

# Check /etc/cron.d/
echo ""
echo -e "${YELLOW}Checking /etc/cron.d/...${NC}"
echo "----------------------------------------"
CRON_D=$(sudo grep -r -i chit /etc/cron.d/ 2>/dev/null || echo "")
if [ -n "$CRON_D" ]; then
    echo -e "${GREEN}✓ Found ChitUI in /etc/cron.d/:${NC}"
    echo "$CRON_D"
else
    echo -e "${YELLOW}No ChitUI entries in /etc/cron.d/${NC}"
fi

# Check .bashrc, .profile, .bash_profile
echo ""
echo -e "${YELLOW}Checking shell startup files...${NC}"
echo "----------------------------------------"
for file in ~/.bashrc ~/.profile ~/.bash_profile; do
    if [ -f "$file" ]; then
        if grep -q -i chit "$file"; then
            echo -e "${GREEN}✓ Found ChitUI in $file:${NC}"
            grep -i chit "$file"
        fi
    fi
done

# Check running processes
echo ""
echo -e "${YELLOW}Checking running processes...${NC}"
echo "----------------------------------------"
PROCS=$(ps aux | grep -i "[m]ain.py\|[c]hitui" || echo "")
if [ -n "$PROCS" ]; then
    echo -e "${GREEN}✓ Found running ChitUI processes:${NC}"
    echo "$PROCS"
else
    echo -e "${YELLOW}No ChitUI processes currently running${NC}"
fi

# Check what's listening on port 8080
echo ""
echo -e "${YELLOW}Checking what's listening on port 8080...${NC}"
echo "----------------------------------------"
PORT_8080=$(sudo netstat -tlnp 2>/dev/null | grep ":8080" || sudo ss -tlnp 2>/dev/null | grep ":8080" || echo "")
if [ -n "$PORT_8080" ]; then
    echo -e "${GREEN}✓ Something is listening on port 8080:${NC}"
    echo "$PORT_8080"
else
    echo -e "${YELLOW}Nothing currently listening on port 8080${NC}"
fi

# Check for supervisor or other process managers
echo ""
echo -e "${YELLOW}Checking for process managers...${NC}"
echo "----------------------------------------"
if command -v supervisorctl &> /dev/null; then
    echo -e "${BLUE}Supervisor found, checking config:${NC}"
    sudo supervisorctl status | grep -i chit || echo "No ChitUI in supervisor"
fi

if [ -d "/etc/supervisor/conf.d" ]; then
    SUPERVISOR_CONF=$(sudo grep -r -i chit /etc/supervisor/conf.d/ 2>/dev/null || echo "")
    if [ -n "$SUPERVISOR_CONF" ]; then
        echo -e "${GREEN}✓ Found ChitUI in supervisor config:${NC}"
        echo "$SUPERVISOR_CONF"
    fi
fi

# Check Docker containers
if command -v docker &> /dev/null; then
    echo ""
    echo -e "${BLUE}Checking Docker containers:${NC}"
    DOCKER_CONTAINERS=$(sudo docker ps -a | grep -i chit || echo "")
    if [ -n "$DOCKER_CONTAINERS" ]; then
        echo -e "${GREEN}✓ Found ChitUI Docker container:${NC}"
        echo "$DOCKER_CONTAINERS"
    fi
fi

# Summary
echo ""
echo -e "${BLUE}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║              Diagnostic Complete             ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Recommendation:${NC}"
echo "If ChitUI is running but no autostart method was found above,"
echo "check the following:"
echo "  1. Desktop autostart: ~/.config/autostart/"
echo "  2. LXDE/XFCE session: Check session startup applications"
echo "  3. Manual start: Someone might be starting it manually"
echo "  4. Custom init.d script: Check /etc/init.d/ for chitui scripts"
echo ""
