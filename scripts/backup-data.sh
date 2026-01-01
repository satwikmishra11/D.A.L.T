# ========== scripts/backup-data.sh ==========
#!/bin/bash
set -e

BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
MONGO_CONTAINER="loadtest-mongodb"

echo "Starting backup at ${TIMESTAMP}..."

mkdir -p ${BACKUP_DIR}

# Backup MongoDB
echo "Backing up MongoDB..."
docker exec ${MONGO_CONTAINER} mongodump \
  --out=/tmp/backup \
  --db=loadtest

docker cp ${MONGO_CONTAINER}:/tmp/backup ${BACKUP_DIR}/mongo_${TIMESTAMP}

echo "✓ MongoDB backup complete"

# Backup Redis (RDB snapshot)
echo "Backing up Redis..."
docker exec loadtest-redis redis-cli SAVE
docker cp loadtest-redis:/data/dump.rdb ${BACKUP_DIR}/redis_${TIMESTAMP}.rdb

echo "✓ Redis backup complete"

# Create archive
echo "Creating archive..."
cd ${BACKUP_DIR}
tar -czf backup_${TIMESTAMP}.tar.gz mongo_${TIMESTAMP} redis_${TIMESTAMP}.rdb
rm -rf mongo_${TIMESTAMP} redis_${TIMESTAMP}.rdb

echo "✓ Backup complete: ${BACKUP_DIR}/backup_${TIMESTAMP}.tar.gz"

# Cleanup old backups (keep last 7 days)
find ${BACKUP_DIR} -name "backup_*.tar.gz" -mtime +7 -delete