######################### DEFINITIONS ############################
# Define the commandline invocation of Maven if necessary:
ifeq ($(MVN),)
    MVN  := mvn
endif

ifeq ($(RELEASE_VERSION),)
    RELEASE_VERSION  := $(shell xmllint --xpath "/*[local-name() = 'project']/*[local-name() = 'version']/text()" pom.xml | perl -pe 's/-SNAPSHOT//')
endif

ifeq ($(NEXT_VERSION),)
    NEXT_VERSION  := $(shell echo $(RELEASE_VERSION) | perl -pe 's{^(([0-9]\.)+)?([0-9]+)$$}{$$1 . ($$3 + 1)}e')
endif

ifneq (,$(findstring -SNAPSHOT,$(RELEASE_VERSION)))
	RELEASE_VERSION_NSNP = $(shell echo $(RELEASE_VERSION) | perl -pe 's/-SNAPSHOT//')
else
	RELEASE_VERSION_NSNP = $(RELEASE_VERSION)
endif

ifeq (,$(findstring -SNAPSHOT,$(NEXT_VERSION)))
	NEXT_VERSION_SNP = $(NEXT_VERSION)-SNAPSHOT
else
	NEXT_VERSION_SNP = $(NEXT_VERSION)
endif
######################## BUILD TARGETS ###########################
# JAVA MC MONITOR
BINARY=mc-monitor-agones-java
APP_NAME=mc-monitor-agones
JAR_NAME="$(APP_NAME)-$(RELEASE_VERSION)-SNAPSHOT.jar"

# GO MC BAKCUP
BACKUP_BINARY=mc-oci-backup

###### Build Docker image ###
DOCKER=$(shell command -v podman || command -v docker)
SHELL = /bin/bash
COMMIT := $(shell git rev-parse --short HEAD)
IMAGE_VERSION := $(shell set -o pipefail; git describe --exact-match --tags HEAD 2> /dev/null | cut -c 2- || echo ${COMMIT})
BUILD_FLAGS ?= -v
ARCH ?= amd64
# import deploy config
# You can change the default deploy config with `make cnf="deploy_special.env" release`
dpl ?= env/deploy.env
include $(dpl)
export $(shell sed 's/=.*//' $(dpl))

IMAGE_LOCAL := ${BINARY}
IMAGE_REMOTE := ${DOCKER_REPO}/$(NAMESPACE)/$(CONTAINER)/$(BINARY)
IMAGE_REMOTE_MCBACKUP := ${DOCKER_REPO}/$(NAMESPACE)/$(CONTAINER)/$(BACKUP_BINARY)

#### Build Java Monitor App ##########
build.monitor: clean.monitor package.monitor

clean.monitor:
	@rm -rf target

package.monitor:
	@echo setting current release: $(RELEASE_VERSION)
	@ $(MVN) $(MVNFLAGS) package -DmcBinaryVersion=${APP_NAME}

version-bump.monitor:
	@echo setting next version: $(NEXT_VERSION_SNP)
	@ $(MVN) versions:set -DgenerateBackupPoms=false -DnewVersion=$(NEXT_VERSION_SNP)

### ACTIONS ####
### JAVA Minecraft Monitor ###
build.docker:
	docker build --rm --tag $(IMAGE_LOCAL):$(IMAGE_VERSION) --build-arg JAVA_APP=$(JAR_NAME) --build-arg ARCH=$(ARCH) .

run: ## run the container
	docker run $(IMAGE_LOCAL):$(IMAGE_VERSION)

release.monitor: build.docker publish.monitor ## Make a release by building and publishing the `{version}` ans `latest` tagged containers to ECR

# Docker publish
publish.monitor: repo-login publish-latest publish-version ## Publish the `{version}` and `latest` tagged images to OCIR

publish-latest: tag-latest ## Publish the `latest` taged container to ECR
	@echo 'publish latest to $(DOCKER_REPO)'
	$(DOCKER) push $(IMAGE_REMOTE):latest


publish-version: tag-version ## Publish the `{version}` taged container to ECR
	@echo 'publish $(VERSION) to $(DOCKER_REPO)'
	$(DOCKER) push $(IMAGE_REMOTE):$(RELEASE_VERSION)

# Docker tagging
tag: tag-latest tag-version ## Generate container tags for the `{version}` ans `latest` tags

tag-latest: ## Generate container `{version}` tag
	@echo 'create tag latest'
	$(DOCKER) tag $(IMAGE_LOCAL):$(IMAGE_VERSION) $(IMAGE_REMOTE):latest

tag-version: ## Generate container `latest` tag
	@echo 'create tag $(VERSION)'
	$(DOCKER) tag $(IMAGE_LOCAL):$(IMAGE_VERSION) $(IMAGE_REMOTE):$(RELEASE_VERSION)
	#$(DOCKER) tag $(IMAGE_LOCAL):$(IMAGE_VERSION) $(IMAGE_REMOTE):pi0.1


####  GO Minecraft Backlup #########
build.mcbackup: clean.mcbackup package.mcbackup

clean.mcbackup:
	@go clean


package.mcbackup:
	@echo buildiing Mcbackup release: $(RELEASE_VERSION)
	CGO_ENABLED=0 GOOS=linux GOARCH=$(ARCH) go build -o build $(BUILD_FLAGS) .

version-bump.mcbackup:
	@echo setting next version: $(NEXT_VERSION_SNP)
	@ $(MVN) versions:set -DgenerateBackupPoms=false -DnewVersion=$(NEXT_VERSION_SNP)

build.docker.mcbackup:
	docker build -f Dockerfile.mc-backup --rm --tag $(BACKUP_BINARY):$(IMAGE_VERSION) --build-arg VERSION=$(IMAGE_VERSION) --build-arg ARCH=$(ARCH) .

release.mcbackup: build.mcbackup publish.mcbackup

publish.mcbackup: repo-login publish-latest.mcbackup

publish-latest.mcbackup:
	$(DOCKER) tag $(BACKUP_BINARY):$(IMAGE_VERSION) $(IMAGE_REMOTE_MCBACKUP):latest
	$(DOCKER) push $(IMAGE_REMOTE_MCBACKUP):latest

publish-version.mcbackup:
	$(DOCKER) tag $(BACKUP_BINARY):$(IMAGE_VERSION) $(IMAGE_REMOTE_MCBACKUP):$(IMAGE_VERSION)
	$(DOCKER) push $(IMAGE_REMOTE_MCBACKUP):$(IMAGE_VERSION)

docker.run:
	docker run $(BACKUP_BINARY):$(IMAGE_VERSION) backup

# HELPERS


# login to AWS-ECR
repo-login: ## Auto login to OCIR.
	#@docker login -u "$(OCIR_USER)" -p "$(OCIR_PASS)" $(DOCKER_REPO)
	@(test -e env/.ocir-cred \
					&& echo "logging into docker" \
					&& tail -n 1 env/.ocir-cred \
					| $(DOCKER) login --password-stdin -u $(shell head -n 1 env/.ocir-cred) $(DOCKER_REPO)) \
                || true


version: ## Output the current version
	@echo $(IMAGE_VERSION)