def projectDir = new File(basedir, "pom.xml").isFile() ? basedir : context.projectDir

// Canonical organization manifest: intentionally fixed, never discovered from generated output.
def requiredFiles = [
    ".dockerignore",
    ".gitattributes",
    ".gitignore",
    ".mvn/wrapper/maven-wrapper.properties",
    "deploy/container/Dockerfile",
    "README.md",
    "lombok.config",
    "mvnw",
    "mvnw.cmd",
    "pom.xml",
    "student-management-organization-adapter/pom.xml",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/controller/GradeController.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/controller/SchoolClassController.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/controller/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/controller/PermissionController.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/controller/RoleController.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/controller/UserController.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/controller/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/converter/GradeAdapterConverter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/converter/PermissionAdapterConverter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/converter/RoleAdapterConverter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/converter/SchoolClassAdapterConverter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/converter/UserAdapterConverter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/converter/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/converter/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/dto/AssignUserToClassRequest.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/dto/CreateGradeRequest.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/dto/CreateSchoolClassMessage.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/dto/CreateSchoolClassRequest.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/dto/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/dto/AssignRoleRequest.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/dto/CreateUserMessage.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/dto/CreateUserRequest.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/dto/GrantPermissionRequest.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/dto/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/impl/OrganizationFacadeSupport.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/impl/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/facade/impl/GradeFacadeImpl.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/facade/impl/SchoolClassFacadeImpl.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/facade/impl/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/facade/impl/PermissionFacadeImpl.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/facade/impl/RoleFacadeImpl.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/facade/impl/UserFacadeImpl.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/facade/impl/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/filter/OrganizationAuthContextFilter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/filter/OrganizationTraceFilter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/filter/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/graphql/OrganizationGraphQlContextInterceptor.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/graphql/SchoolClassResolver.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/graphql/UserResolver.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/graphql/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/handler/OrganizationErrorResponse.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/handler/OrganizationGlobalExceptionHandler.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/handler/OrganizationGraphQlExceptionResolver.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/handler/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/mq/OrganizationMessageSupport.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/mq/RetryableOrganizationMessageException.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/mq/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/mq/SchoolClassChangedConsumer.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/mq/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/mq/UserCreatedConsumer.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/mq/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/rpc/SchoolClassRpcProvider.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/rpc/UserRpcProvider.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/graphql/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/rpc/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/graphql/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/rpc/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/vo/GradeDetailVO.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/vo/SchoolClassDetailVO.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/vo/package-info.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/vo/PermissionTreeVO.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/vo/UserDetailVO.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/user/vo/package-info.java",
    "student-management-organization-adapter/src/main/resources/.gitkeep",
    "student-management-organization-adapter/src/main/resources/graphql/schema.graphqls",
    "student-management-organization-adapter/src/test/java/it/pkg/.gitkeep",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/OrganizationDubboProviderConfigurationTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/OrganizationFilterTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/OrganizationGraphQlContractTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/OrganizationHttpErrorContractTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/OrganizationRabbitMqConsumerTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/user/controller/RolePermissionControllerTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/teaching/controller/TeachingControllerTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/user/controller/UserControllerTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/package-info.java",
    "student-management-organization-adapter/src/test/resources/.gitkeep",
    "student-management-organization-application/pom.xml",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/assemblers/GradeAssembler.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/assemblers/SchoolClassAssembler.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/assemblers/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/assemblers/PermissionAssembler.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/assemblers/UserAssembler.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/assemblers/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/command/AssignUserToClassCommand.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/command/CreateGradeCommand.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/command/CreateSchoolClassCommand.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/command/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/command/AssignRoleCommand.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/command/CreateUserCommand.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/command/GrantPermissionCommand.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/command/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/config/DomainServiceConfiguration.java",
    "student-management-organization-application/src/main/java/it/pkg/application/config/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/context/OrganizationRequestContext.java",
    "student-management-organization-application/src/main/java/it/pkg/application/context/OrganizationRequestContextHolder.java",
    "student-management-organization-application/src/main/java/it/pkg/application/context/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/converter/GradeApplicationConverter.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/converter/SchoolClassApplicationConverter.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/converter/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/converter/PermissionApplicationConverter.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/converter/RoleApplicationConverter.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/converter/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/exceptions/OrganizationApplicationException.java",
    "student-management-organization-application/src/main/java/it/pkg/application/exceptions/OrganizationFailureType.java",
    "student-management-organization-application/src/main/java/it/pkg/application/exceptions/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/GradeManage.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/SchoolClassManage.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/impl/GradeManageImpl.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/impl/SchoolClassManageImpl.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/impl/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/manage/PermissionManage.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/manage/RoleManage.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/manage/UserManage.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/manage/impl/PermissionManageImpl.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/manage/impl/RoleManageImpl.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/manage/impl/UserManageImpl.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/manage/impl/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/manage/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/query/GradeDetailQuery.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/query/SchoolClassDetailQuery.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/query/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/query/PermissionTreeQuery.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/query/UserDetailQuery.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/query/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/result/GradeDetailResult.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/result/SchoolClassDetailResult.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/result/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/result/PermissionTreeResult.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/result/UserDetailResult.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/result/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/support/IdempotentCommand.java",
    "student-management-organization-application/src/main/java/it/pkg/application/support/OrganizationTransactionHooks.java",
    "student-management-organization-application/src/main/java/it/pkg/application/support/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/validators/GradeApplicationValidator.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/validators/TeachingApplicationValidator.java",
    "student-management-organization-application/src/main/java/it/pkg/application/teaching/validators/package-info.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/validators/PermissionApplicationValidator.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/validators/UserApplicationValidator.java",
    "student-management-organization-application/src/main/java/it/pkg/application/user/validators/package-info.java",
    "student-management-organization-application/src/main/resources/.gitkeep",
    "student-management-organization-application/src/test/java/it/pkg/.gitkeep",
    "student-management-organization-application/src/test/java/it/pkg/application/teaching/AssignUserToClassUseCaseTest.java",
    "student-management-organization-application/src/test/java/it/pkg/application/teaching/GradeManageImplTest.java",
    "student-management-organization-application/src/test/java/it/pkg/application/teaching/SchoolClassManageImplTest.java",
    "student-management-organization-application/src/test/java/it/pkg/application/teaching/package-info.java",
    "student-management-organization-application/src/test/java/it/pkg/application/user/PermissionManageImplTest.java",
    "student-management-organization-application/src/test/java/it/pkg/application/user/RoleManageImplTest.java",
    "student-management-organization-application/src/test/java/it/pkg/application/user/UserManageImplTest.java",
    "student-management-organization-application/src/test/java/it/pkg/application/user/package-info.java",
    "student-management-organization-application/src/test/resources/.gitkeep",
    "student-management-organization-common/pom.xml",
    "student-management-organization-common/src/main/java/it/pkg/common/constants/ErrorCodes.java",
    "student-management-organization-common/src/main/java/it/pkg/common/constants/package-info.java",
    "student-management-organization-common/src/main/java/it/pkg/common/exceptions/BizException.java",
    "student-management-organization-common/src/main/java/it/pkg/common/exceptions/NotFoundException.java",
    "student-management-organization-common/src/main/java/it/pkg/common/exceptions/package-info.java",
    "student-management-organization-common/src/main/java/it/pkg/common/utils/IdGenerator.java",
    "student-management-organization-common/src/main/java/it/pkg/common/utils/package-info.java",
    "student-management-organization-common/src/main/resources/.gitkeep",
    "student-management-organization-common/src/test/java/it/pkg/.gitkeep",
    "student-management-organization-common/src/test/resources/.gitkeep",
    "student-management-organization-domain/pom.xml",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/aggregates/SchoolClassAggregate.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/aggregates/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/aggregates/RolePermissionAggregate.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/aggregates/UserAggregate.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/aggregates/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/client/CommandIdempotencyPort.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/client/OrganizationEventPublisher.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/client/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/client/GradeCachePort.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/client/SchoolClassCachePort.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/client/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/client/UserCachePort.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/client/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/entities/Grade.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/entities/SchoolClass.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/entities/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/entities/Permission.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/entities/Role.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/entities/User.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/entities/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/enums/GradeStatus.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/enums/SchoolClassStatus.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/enums/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/enums/PermissionStatus.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/enums/PermissionType.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/enums/RoleStatus.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/enums/UserStatus.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/enums/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/events/OrganizationDomainEvent.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/events/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/events/GradeChangedEvent.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/events/SchoolClassChangedEvent.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/events/SchoolClassMembershipChangedEvent.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/events/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/events/PermissionGrantedEvent.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/events/RoleAssignedEvent.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/events/UserChangedEvent.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/events/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/exceptions/OrganizationDomainErrorCode.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/exceptions/OrganizationDomainException.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/exceptions/OrganizationPortException.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/exceptions/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/repos/GradeRepository.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/repos/SchoolClassRepository.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/repos/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/repos/PermissionRepository.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/repos/RoleRepository.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/repos/UserRepository.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/repos/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/service/GradeDomainService.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/service/SchoolClassDomainService.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/service/impl/GradeDomainServiceImpl.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/service/impl/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/service/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/service/PermissionDomainService.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/service/UserDomainService.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/service/impl/PermissionDomainServiceImpl.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/service/impl/UserDomainServiceImpl.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/service/impl/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/service/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/validators/OrganizationCodeValidator.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/validators/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/validators/TeachingDomainValidator.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/validators/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/validators/UserDomainValidator.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/validators/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/vos/GradeCode.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/vos/SchoolClassId.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/vos/package-info.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/vos/PermissionCode.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/vos/RoleCode.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/vos/UserId.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/vos/package-info.java",
    "student-management-organization-domain/src/main/resources/.gitkeep",
    "student-management-organization-domain/src/test/java/it/pkg/.gitkeep",
    "student-management-organization-domain/src/test/java/it/pkg/domain/teaching/GradeDomainServiceTest.java",
    "student-management-organization-domain/src/test/java/it/pkg/domain/teaching/SchoolClassDomainServiceTest.java",
    "student-management-organization-domain/src/test/java/it/pkg/domain/teaching/package-info.java",
    "student-management-organization-domain/src/test/java/it/pkg/domain/user/RolePermissionAggregateTest.java",
    "student-management-organization-domain/src/test/java/it/pkg/domain/user/UserDomainServiceTest.java",
    "student-management-organization-domain/src/test/java/it/pkg/domain/user/package-info.java",
    "student-management-organization-domain/src/test/resources/.gitkeep",
    "student-management-organization-facade/pom.xml",
    "student-management-organization-facade/src/main/java/it/pkg/facade/teaching/dto/AssignUserToClassDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/teaching/dto/CreateGradeDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/teaching/dto/CreateSchoolClassDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/teaching/dto/GradeDetailDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/teaching/dto/SchoolClassDetailDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/teaching/dto/package-info.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/user/dto/AssignRoleDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/user/dto/CreateUserDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/user/dto/GrantPermissionDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/user/dto/PermissionTreeDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/user/dto/UserDetailDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/user/dto/package-info.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/exceptions/OrganizationFacadeException.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/exceptions/package-info.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/teaching/GradeFacade.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/teaching/SchoolClassFacade.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/teaching/package-info.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/user/PermissionFacade.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/user/RoleFacade.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/user/UserFacade.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/user/package-info.java",
    "student-management-organization-facade/src/main/resources/.gitkeep",
    "student-management-organization-facade/src/test/java/it/pkg/.gitkeep",
    "student-management-organization-facade/src/test/java/it/pkg/facade/OrganizationFacadeContractTest.java",
    "student-management-organization-facade/src/test/java/it/pkg/facade/package-info.java",
    "student-management-organization-facade/src/test/resources/.gitkeep",
    "student-management-organization-infrastructure/pom.xml",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/aop/OrganizationLogAspect.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/aop/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/cache/InMemoryCommandIdempotencyAdapter.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/cache/InMemoryGradeCache.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/cache/InMemorySchoolClassCache.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/cache/InMemoryUserCache.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/cache/OrganizationCacheKey.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/cache/RedisCommandIdempotencyAdapter.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/cache/RedisGradeCache.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/cache/RedisSchoolClassCache.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/cache/RedisUserCache.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/cache/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/config/OrganizationIntegrationProperties.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/config/OrganizationLocalFallbackConfig.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/config/OrganizationRabbitConfig.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/config/OrganizationRedisConfig.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/config/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/mq/LocalOrganizationEventPublisher.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/mq/OrganizationEventMessage.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/mq/OrganizationEventProducer.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/mq/RabbitOrganizationEventPublisher.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/mq/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/converter/GradePOConverter.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/converter/SchoolClassPOConverter.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/converter/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/impl/GradeRepositoryImpl.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/impl/SchoolClassRepositoryImpl.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/impl/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/jpa/GradeJpaRepository.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/jpa/SchoolClassJpaRepository.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/jpa/SchoolClassUserJpaRepository.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/jpa/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/po/GradePO.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/po/SchoolClassPO.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/po/SchoolClassUserPO.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/po/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/converter/PermissionPOConverter.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/converter/RolePOConverter.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/converter/UserPOConverter.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/converter/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/impl/PermissionRepositoryImpl.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/impl/RoleRepositoryImpl.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/impl/UserRepositoryImpl.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/impl/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/jpa/PermissionJpaRepository.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/jpa/RoleJpaRepository.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/jpa/RolePermissionJpaRepository.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/jpa/UserJpaRepository.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/jpa/UserRoleJpaRepository.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/jpa/package-info.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/PermissionPO.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/RolePO.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/RolePermissionPO.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/UserPO.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/UserRolePO.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/package-info.java",
    "student-management-organization-infrastructure/src/main/resources/db/migration/V1__init_student_management_organization.sql",
    "student-management-organization-infrastructure/src/main/resources/db/migration/V2__complete_organization_domains.sql",
    "student-management-organization-infrastructure/src/test/java/it/pkg/.gitkeep",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/OrganizationCacheTest.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/OrganizationFlywayMigrationTest.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/OrganizationInfrastructureProfileTest.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/OrganizationLogAspectTest.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/OrganizationRabbitMqContractTest.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/package-info.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/teaching/repo/GradeRepositoryImplTest.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/teaching/repo/SchoolClassRepositoryImplTest.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/teaching/repo/package-info.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/user/repo/RolePermissionRepositoryImplTest.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/user/repo/UserRepositoryImplTest.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/user/repo/package-info.java",
    "student-management-organization-infrastructure/src/test/resources/.gitkeep",
    "student-management-organization-starter/pom.xml",
    "student-management-organization-starter/src/main/java/it/pkg/starter/OrganizationApplication.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/OrganizationActuatorConfig.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/OrganizationJacksonConfig.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/OrganizationSwaggerConfig.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/async/AsyncConfiguration.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/async/package-info.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptor.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigCipherCli.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptException.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptKeyProvider.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptor.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/package-info.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/package-info.java",
    "student-management-organization-starter/src/main/java/it/pkg/starter/package-info.java",
    "student-management-organization-starter/src/main/resources/META-INF/spring.factories",
    "student-management-organization-starter/src/main/resources/application-dev.yml",
    "student-management-organization-starter/src/main/resources/application-local.yml",
    "student-management-organization-starter/src/main/resources/application-prod.yml",
    "student-management-organization-starter/src/main/resources/application-test.yml",
    "student-management-organization-starter/src/main/resources/application.yml",
    "student-management-organization-starter/src/main/resources/bootstrap-dev.yml",
    "student-management-organization-starter/src/main/resources/bootstrap-local.yml",
    "student-management-organization-starter/src/main/resources/bootstrap-prod.yml",
    "student-management-organization-starter/src/main/resources/bootstrap-test.yml",
    "student-management-organization-starter/src/main/resources/bootstrap.yml",
    "student-management-organization-starter/src/main/resources/logback-spring.xml",
    "student-management-organization-starter/src/test/java/it/pkg/starter/ArchitectureDependencyTest.java",
    "student-management-organization-starter/src/test/java/it/pkg/starter/OrganizationApplicationTest.java",
    "student-management-organization-starter/src/test/java/it/pkg/starter/OrganizationFlowTest.java",
    "student-management-organization-starter/src/test/java/it/pkg/starter/OrganizationOpenApiTest.java",
    "student-management-organization-starter/src/test/java/it/pkg/starter/OrganizationRollbackTest.java",
    "student-management-organization-starter/src/test/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptorTest.java",
    "student-management-organization-starter/src/test/java/it/pkg/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java",
    "student-management-organization-starter/src/test/java/it/pkg/starter/config/encryption/package-info.java",
    "student-management-organization-starter/src/test/java/it/pkg/starter/package-info.java",
    "student-management-organization-starter/src/test/resources/.gitkeep",
    "student-management-organization-starter/src/test/resources/logback-test.xml",
].sort()
assert requiredFiles.size() == requiredFiles.toSet().size()

def assertFile = { path ->
    def file = new File(projectDir, path)
    assert file.isFile(): "Expected file ${path}"
    file
}

def assertDir = { path ->
    def file = new File(projectDir, path)
    assert file.isDirectory(): "Expected directory ${path}"
    file
}

def assertRuntimeConfigFiles = { resourcesDir ->
    [
        "bootstrap.yml",
        "bootstrap-local.yml",
        "bootstrap-dev.yml",
        "bootstrap-test.yml",
        "bootstrap-prod.yml",
        "application.yml",
        "application-local.yml",
        "application-dev.yml",
        "application-test.yml",
        "application-prod.yml"
    ].each {
        assertFile("${resourcesDir}/${it}")
    }
}

def javaFileTexts = { path ->
    def dir = new File(projectDir, path)
    assert dir.isDirectory(): "Expected directory ${path}"
    def files = []
    dir.traverse(type: groovy.io.FileType.FILES) { file ->
        if (file.name.endsWith(".java")) {
            files << file
        }
    }
    files.collect { it.text }
}

def assertNoGenericMapStructConverterInjection = { path ->
    javaFileTexts(path).each { text ->
        assert !text.contains("import io.github.linpeilie.Converter;")
        assert !text.contains("private final Converter converter;")
        assert !text.contains('@Qualifier("converter")')
    }
}

def assertNoJavaText = { path, token ->
    def matches = []
    def dir = new File(projectDir, path)
    assert dir.isDirectory(): "Expected directory ${path}"
    dir.traverse(type: groovy.io.FileType.FILES) { file ->
        if (file.name.endsWith(".java") && file.getText("UTF-8").contains(token)) {
            matches << projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
        }
    }
    assert matches.isEmpty(): "Unexpected token '${token}' in ${matches.join(', ')}"
}

def assertMissing = { path ->
    assert !new File(projectDir, path).exists(): "Unexpected stale path ${path}"
}

def assertPortableDockerfile = { jarFile, exposedPorts, readinessPort ->
    assertMissing("Dockerfile")
    [
        "Dockerfile.containerd",
        "Dockerfile.nerdctl",
        "Dockerfile.podman",
        "Containerfile",
        "Containerfile.podman",
        "deploy/container/Dockerfile.containerd",
        "deploy/container/Dockerfile.nerdctl",
        "deploy/container/Dockerfile.podman",
        "deploy/container/Containerfile",
        "deploy/container/Containerfile.podman"
    ].each { assertMissing(it) }

    def text = assertFile("deploy/container/Dockerfile").text
    assert text.contains("ARG BUILD_IMAGE=eclipse-temurin:21-jdk-jammy")
    assert text.contains('FROM ${BUILD_IMAGE} AS builder')
    assert text.contains("chmod +x mvnw")
    assert text.contains("./mvnw -B -ntp -DskipTests package")
    assert text.contains("ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy")
    assert text.contains('FROM ${RUNTIME_IMAGE} AS extractor')
    assert text.contains("ARG JAR_FILE=${jarFile}")
    assert text.contains('COPY --from=builder /workspace/${JAR_FILE} app.jar')
    assert text.contains("java -Djarmode=tools -jar app.jar extract --layers --destination extracted")
    assert text.contains('FROM ${RUNTIME_IMAGE} AS runtime')
    assert text.contains("ARG CONTAINER_ENGINE=oci")
    assert text.contains("ARG APP_UID=10001")
    assert text.contains("ARG APP_GID=10001")
    assert text.contains("org.opencontainers.image.build.engine")
    assert text.contains("USER app")
    assert text.contains("EXPOSE ${exposedPorts}")
    assert text.contains("http://127.0.0.1:${readinessPort}/actuator/health/readiness")
    assert text.contains("JarLauncher")
    assert !text.contains("--mount=type=cache")
}

def webDomainModule = "student-management-organization-domain/src/main/java/it/pkg/domain"
["user", "teaching"].each { businessDomain ->
    ["aggregates", "client", "entities", "enums", "events", "repos", "service", "validators", "vos"].each { role ->
        assertFile("${webDomainModule}/${businessDomain}/${role}/package-info.java")
        assertMissing("${webDomainModule}/${role}/${businessDomain}")
    }
}
assertFile("${webDomainModule}/client/evaluation/package-info.java")
assertFile("${webDomainModule}/events/OrganizationDomainEvent.java")

def webApplicationModule = "student-management-organization-application/src/main/java/it/pkg/application"
["user", "teaching"].each { businessDomain ->
    ["assemblers", "command", "converter", "manage", "query", "result", "validators"].each { role ->
        assertFile("${webApplicationModule}/${businessDomain}/${role}/package-info.java")
        assertMissing("${webApplicationModule}/${role}/${businessDomain}")
    }
    assertFile("${webApplicationModule}/${businessDomain}/manage/impl/package-info.java")
}

def webInfrastructureModule = "student-management-organization-infrastructure"
["user", "teaching"].each { businessDomain ->
    assertFile("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/${businessDomain}/repo/package-info.java")
    assertMissing("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/repo/${businessDomain}")
    assertFile("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/${businessDomain}/cache/package-info.java")
    assertFile("${webInfrastructureModule}/src/test/java/it/pkg/infrastructure/${businessDomain}/repo/package-info.java")
}
assertFile("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/cache/InMemoryCommandIdempotencyAdapter.java")
assertFile("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/cache/RedisCommandIdempotencyAdapter.java")
assertFile("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/cache/OrganizationCacheKey.java")
assertMissing("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/cache/InMemoryUserCache.java")
assertMissing("${webInfrastructureModule}/src/main/java/it/pkg/infrastructure/cache/InMemoryGradeCache.java")

[
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/user/controller/UserControllerTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/user/controller/RolePermissionControllerTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/user/controller/package-info.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/teaching/controller/TeachingControllerTest.java",
    "student-management-organization-adapter/src/test/java/it/pkg/adapter/teaching/controller/package-info.java"
].each { assertFile(it) }

[
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/controller/user",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/controller/teaching",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/dto/user",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/dto/teaching",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/vo/user",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/vo/teaching",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/impl/user",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/impl/teaching",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/mq/user",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/mq/teaching",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/converter/UserAdapterConverter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/converter/RoleAdapterConverter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/converter/PermissionAdapterConverter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/converter/GradeAdapterConverter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/converter/SchoolClassAdapterConverter.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/rpc/UserRpcProvider.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/rpc/SchoolClassRpcProvider.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/graphql/UserResolver.java",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/graphql/SchoolClassResolver.java"
].each { assertMissing(it) }

requiredFiles.addAll([
    "student-management-organization-domain/src/main/java/it/pkg/domain/client/ExternalDependencyFailure.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/client/ExternalDependencyException.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/client/evaluation/EvaluationQueryPort.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/client/evaluation/EvaluationCourse.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/client/evaluation/EvaluationExam.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/client/evaluation/EvaluationScore.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/evaluation/DubboEvaluationQueryClient.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/evaluation/EvaluationClientFailureMapper.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/evaluation/LocalEvaluationQueryStub.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/client/evaluation/DubboEvaluationQueryClientTest.java",
    "student-management-organization-infrastructure/src/test/java/it/pkg/infrastructure/client/evaluation/LocalEvaluationQueryStubTest.java"
])
requiredFiles.each { assertFile(it) }

[
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/user",
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/teaching",
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/package-info.java"
].each { assertMissing(it) }
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/dto/CreateUserDTO.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/teaching/dto/SchoolClassDetailDTO.java")

def forbiddenPaths = [
    "student-management-evaluation",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/user",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/teaching",
    "student-management-organization-adapter/src/main/java/it/pkg/adapter/validation",
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/PageResponse.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/user/CreateUserRequest.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/user/UserDTO.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/teaching/CreateSchoolClassRequest.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/teaching/AssignUserToClassRequest.java",
    "student-management-organization-facade/src/main/java/it/pkg/facade/dto/teaching/SchoolClassDTO.java",
    "student-management-organization-common/src/main/java/it/pkg/common/response",
    "student-management-organization-domain/src/main/java/it/pkg/domain/common",
    "student-management-organization-domain/src/main/java/it/pkg/domain/enums/UserStatus.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/client/EvaluationClient.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/client/CourseClient.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/user/client/UserClient.java",
    "student-management-organization-domain/src/main/java/it/pkg/domain/teaching/client/SchoolClassClient.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/EvaluationClientImpl.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/user/UserClientImpl.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/teaching/SchoolClassClientImpl.java",
    "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/config/OrganizationMybatisPlusConfig.java",
    "student-management-organization-infrastructure/src/main/resources/mapper",
    "student-management-organization-infrastructure/src/test/resources/db/migration",
    "student-management-organization-starter/src/main/java/it/pkg/starter/config/OrganizationOpenApiConfig.java"
].sort().unique()
forbiddenPaths.each { assertMissing(it) }

def forbiddenMatches = []
projectDir.traverse(type: groovy.io.FileType.FILES) { file ->
    def path = projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
    if (path.contains("/src/") && path.contains("/java/") && path.contains("/convertor/")) {
        forbiddenMatches << path
    }
    if (path.contains("/src/") && path.contains("/java/") && path.contains("/facade/impl/")
            && !path.startsWith("student-management-organization-adapter/")) {
        forbiddenMatches << path
    }
    if (path.startsWith("student-management-organization-infrastructure/src/main/java/")
            && (path.contains("/mp/")
            || (file.name.endsWith("Mapper.java") && !path.contains("/client/")))) {
        forbiddenMatches << path
    }
    if (path.contains("/src/") && path.contains("/java/") && file.name.endsWith("Po.java")) {
        forbiddenMatches << path
    }
}
assert forbiddenMatches.isEmpty(): "Unexpected forbidden generated paths: ${forbiddenMatches.join(', ')}"

def assertPackageDocs = { String sourceRoot ->
    def root = assertDir(sourceRoot)
    def javaDirs = [] as Set
    root.traverse(type: groovy.io.FileType.FILES) { file ->
        if (file.name.endsWith(".java") && file.name != "package-info.java") {
            javaDirs << file.parentFile
        }
    }
    javaDirs.each { dir ->
        assert new File(dir, "package-info.java").isFile():
                "Missing package-info.java in ${projectDir.toPath().relativize(dir.toPath())}"
    }
}

["common", "facade", "domain", "application", "infrastructure", "adapter", "starter"].each { module ->
    assertPackageDocs("student-management-organization-${module}/src/main/java")
    assertPackageDocs("student-management-organization-${module}/src/test/java")
}

def relativePath = { file ->
    projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
}

def isGeneratedOrVcsPath = { path ->
    path.tokenize("/").any { it == "target" || it == ".git" }
}

def sourceConfigDocNames = ["README.md", "pom.xml", ".gitignore", ".gitattributes"] as Set
def sourceConfigDocExtensions = [".java", ".xml", ".yml", ".yaml", ".properties", ".md", ".sql"]
def isSourceConfigDocFile = { file ->
    def path = relativePath(file)
    if (isGeneratedOrVcsPath(path)) {
        return false
    }
    sourceConfigDocNames.contains(file.name) || sourceConfigDocExtensions.any { file.name.endsWith(it) }
}

def collectSourceConfigDocFiles
collectSourceConfigDocFiles = { dir, files ->
    dir.listFiles()?.each { file ->
        def path = relativePath(file)
        if (file.isDirectory()) {
            if (!isGeneratedOrVcsPath(path)) {
                collectSourceConfigDocFiles(file, files)
            }
        } else if (isSourceConfigDocFile(file)) {
            files << file
        }
    }
}

def assertNoStaleText = { files, token ->
    def matches = []
    files.each { file ->
        if (matches.size() < 5 && file.getText("UTF-8").contains(token)) {
            matches << relativePath(file)
        }
    }
    assert matches.isEmpty(): "Unexpected stale token '${token}' in first matching relative paths: ${matches.join(', ')}"
}

assertFile("pom.xml")
def mvnw = assertFile("mvnw")
assertFile("mvnw.cmd")
if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
    assert mvnw.canExecute(): "Expected mvnw to be executable"
}
assertFile(".mvn/wrapper/maven-wrapper.properties")
assertFile(".gitignore")
assertFile(".gitattributes")
assertFile("README.md")
assertFile(".dockerignore")
assert assertFile("README.md").text.contains("Docker")
assert assertFile("README.md").text.contains("## Modules")
assert assertFile("README.md").text.contains("## Domains")
assert assertFile("README.md").text.contains("## Dependency Direction")
assert assertFile("README.md").text.contains("## Integration Ownership")
assert assertFile("README.md").text.contains("## Commands")
assert assertFile("README.md").text.contains("## Runtime Profiles")
assert assertFile("README.md").text.contains("docker build -t student-management-organization:local .")
assertPortableDockerfile(
        "student-management-organization-starter/target/*.jar", "8080 50051", "8080")
def dockerignoreLines = assertFile(".dockerignore").readLines("UTF-8")
[
    ".git",
    ".gitignore",
    ".github",
    ".idea",
    ".vscode",
    "*.iml",
    ".DS_Store",
    "",
    "**/target",
    "**/build",
    "**/.mvn/wrapper/maven-wrapper.jar",
    "",
    "logs",
    "*.log",
    "",
    ".env",
    ".env.*",
    "config/*secret*",
    "secrets",
    "*.pem",
    "*.key"
].each {
    assert dockerignoreLines.contains(it): "Expected .dockerignore to contain line ${it}"
}
def gitignoreLines = assertFile(".gitignore").readLines("UTF-8")
[
    ".env",
    ".env.*",
    "!deploy/env/.env.example",
    "!deploy/env/.env.prod.example",
    "config/application-secrets.yml",
    "secrets/",
    "*.pem",
    "*.key"
].each {
    assert gitignoreLines.contains(it): "Expected .gitignore to contain line ${it}"
}

["common", "facade", "domain", "application", "infrastructure", "adapter", "starter"].each { module ->
    def root = "student-management-organization-${module}"
    assertDir(root)
    ["src/main/java", "src/main/resources", "src/test/java", "src/test/resources"].each { sourceDirectory ->
        assertDir("${root}/${sourceDirectory}")
    }
}

[
    "student-management-organization-client",
    "student-management-organization-app",
    "start",
    "student-management-evaluation"
].each {
    assertMissing(it)
}

[
    "student-management-organization-adapter/src/main/java/mobile",
    "student-management-organization-adapter/src/main/java/wap",
    "student-management-organization-adapter/src/main/java/web",
    "student-management-organization-domain/src/test/java/domain",
    "student-management-organization-infrastructure/src/main/java/config",
    "student-management-organization-infrastructure/src/main/java/customer",
    "student-management-organization-infrastructure/src/main/java/order",
    "student-management-organization-infrastructure/src/main/resources/mybatis",
    "student-management-organization-infrastructure/src/test/java/repository",
    "student-management-organization-infrastructure/src/test/resources/sample.properties"
].each {
    assertMissing(it)
}

assertMissing("student-management-organization-adapter/src/main/java/package-info.java")
assertMissing("student-management-organization-adapter/src/main/java/it/pkg/package-info.java")
assertMissing("student-management-organization-domain/src/main/java/domain")
assertMissing("student-management-organization-domain/src/main/java/it/pkg/domain/package-info.java")
assertMissing("student-management-organization-infrastructure/src/main/java/package-info.java")
assertMissing("student-management-organization-infrastructure/src/main/java/it/pkg/package-info.java")

def rootPomText = assertFile("pom.xml").text
assert rootPomText.contains("<evaluation-facade.group-id>fixture.evaluation</evaluation-facade.group-id>")
assert rootPomText.contains("<evaluation-facade.artifact-id>student-management-evaluation-facade</evaluation-facade.artifact-id>")
assert rootPomText.contains("<evaluation-facade.version>1.0.0-fixture</evaluation-facade.version>")
assert rootPomText.contains("<evaluation-facade.package>fixture.evaluation</evaluation-facade.package>")
assert rootPomText.contains("<artifactId>spring-boot-starter-parent</artifactId>")
assert rootPomText.contains("<version>3.5.16</version>")
assert rootPomText.contains("<java.version>21</java.version>")
assert rootPomText.contains("<lombok.version>1.18.38</lombok.version>")
assert rootPomText.contains("<mapstruct-plus.version>1.5.1</mapstruct-plus.version>")
assert rootPomText.contains("<dubbo.version>3.3.6</dubbo.version>")
assert rootPomText.contains("<spring-cloud.version>2025.0.3</spring-cloud.version>")
assert rootPomText.contains("<spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>")
assert rootPomText.contains("<artifactId>egon-cola-components-bom</artifactId>")
assert rootPomText.contains("<egon-cola.version>5.2.2</egon-cola.version>")
assert !rootPomText.contains("<artifactId>egon-cola-component-common</artifactId>")
assert !rootPomText.contains("<artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>")
assert !rootPomText.contains("<artifactId>egon-cola-component-dynamic-thread-pool-admin</artifactId>")
assert !rootPomText.contains("<artifactId>egon-cola-component-dynamic-thread-pool-test</artifactId>")
def commonPomText = assertFile("student-management-organization-common/pom.xml").text
assert commonPomText.contains("<artifactId>egon-cola-component-common-core</artifactId>")
def generatedPomTexts = []
projectDir.traverse(type: groovy.io.FileType.FILES) { file ->
    def path = relativePath(file)
    if (file.name == "pom.xml" && !isGeneratedOrVcsPath(path)) {
        generatedPomTexts << file.getText("UTF-8")
    }
}
[
    "egon-cola-component-dynamic-thread-pool-starter",
    "egon-cola-component-dynamic-thread-pool-admin",
    "egon-cola-component-dynamic-thread-pool-test"
].each { artifactId ->
    assert !generatedPomTexts.any { it.contains("<artifactId>${artifactId}</artifactId>") }
}
assert rootPomText.contains("<artifactId>spring-cloud-dependencies</artifactId>")
assert rootPomText.contains("<artifactId>spring-cloud-alibaba-dependencies</artifactId>")
assert rootPomText.contains("<artifactId>dubbo-bom</artifactId>")
assert rootPomText.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assert rootPomText.contains("<artifactId>mapstruct-plus-processor</artifactId>")
assert rootPomText.contains("<module>student-management-organization-common</module>")
assert rootPomText.contains("<module>student-management-organization-starter</module>")
assert !rootPomText.contains("spring-ai")
assert !rootPomText.contains("drools")
assert !rootPomText.contains("mcp")

assertFile("lombok.config").text.contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier")

assertRuntimeConfigFiles("student-management-organization-starter/src/main/resources")

def webApplicationYaml = assertFile("student-management-organization-starter/src/main/resources/application.yml").text
assert webApplicationYaml.contains("threads:")
assert webApplicationYaml.contains("virtual:")
assert webApplicationYaml.contains('${SPRING_THREADS_VIRTUAL_ENABLED:true}')
assert webApplicationYaml.contains("timeout-per-shutdown-phase")
assert webApplicationYaml.contains("write-dates-as-timestamps: false")
assert webApplicationYaml.contains("prometheus")
assert webApplicationYaml.contains("tomcat:")
assert webApplicationYaml.contains('${TOMCAT_MAX_CONNECTIONS:8192}')
assert webApplicationYaml.contains("dubbo:")
assert webApplicationYaml.contains('name: ${spring.application.name}')
assert webApplicationYaml.contains('${DUBBO_REGISTRY_ADDRESS:N/A}')
assert webApplicationYaml.contains("name: tri")
assert webApplicationYaml.contains('${DUBBO_PORT:50051}')
assert webApplicationYaml.contains("timeout: 3000")
assert webApplicationYaml.contains("retries: 0")
assert webApplicationYaml.contains('DUBBO_CONSUMER_TIMEOUT:3000')
assert webApplicationYaml.contains('EVALUATION_FACADE_ENABLED:false')

def webApplicationDevYaml = assertFile("student-management-organization-starter/src/main/resources/application-dev.yml").text
def webApplicationProdYaml = assertFile("student-management-organization-starter/src/main/resources/application-prod.yml").text
def webBootstrapDevYaml = assertFile("student-management-organization-starter/src/main/resources/bootstrap-dev.yml").text
def webBootstrapProdYaml = assertFile("student-management-organization-starter/src/main/resources/bootstrap-prod.yml").text
def webApplicationLocalYaml = assertFile("student-management-organization-starter/src/main/resources/application-local.yml").text
def webApplicationTestYaml = assertFile("student-management-organization-starter/src/main/resources/application-test.yml").text
def webBootstrapLocalYaml = assertFile("student-management-organization-starter/src/main/resources/bootstrap-local.yml").text
def webBootstrapTestYaml = assertFile("student-management-organization-starter/src/main/resources/bootstrap-test.yml").text
assert webApplicationDevYaml.contains('password: ${DB_PASSWORD:ENC(')
assert webApplicationProdYaml.contains('password: ${DB_PASSWORD:ENC(')
assert webBootstrapDevYaml.contains('password: ${NACOS_PASSWORD:ENC(')
assert webBootstrapProdYaml.contains('password: ${NACOS_PASSWORD:ENC(')
assert webApplicationLocalYaml.contains('password: ${DB_PASSWORD:}')
assert webApplicationTestYaml.contains('password: ${DB_PASSWORD:}')
assert !webBootstrapLocalYaml.contains('ENC(')
assert !webBootstrapTestYaml.contains('ENC(')
assert webApplicationLocalYaml.contains("evaluation:\n      enabled: false")
assert webApplicationTestYaml.contains("evaluation:\n      enabled: false")
assert webApplicationDevYaml.contains('EVALUATION_FACADE_ENABLED:true')
assert webApplicationProdYaml.contains('EVALUATION_FACADE_ENABLED:true')

def wrapper = assertFile(".mvn/wrapper/maven-wrapper.properties").text
assert wrapper.contains("apache-maven/3.9.14/apache-maven-3.9.14-bin.zip")

def modulePom = { module ->
    new groovy.xml.XmlSlurper(false, false).parse(assertFile("student-management-organization-${module}/pom.xml"))
}

def dependencies = { moduleXml ->
    moduleXml.dependencies.dependency.collect {
        [
            artifactId: it.artifactId.text(),
            scope: it.scope.text() ?: "compile"
        ]
    }
}

def assertDependency = { deps, artifactId ->
    assert deps.any { it.artifactId == artifactId }: "Expected dependency ${artifactId}"
}

def assertScopedDependency = { deps, artifactId, scope ->
    assert deps.any { it.artifactId == artifactId && it.scope == scope }: "Expected dependency ${artifactId} with scope ${scope}"
}

def assertNoDependency = { deps, artifactId ->
    assert !deps.any { it.artifactId == artifactId }: "Unexpected dependency ${artifactId}"
}

def assertSpringBootLayeredJarPlugin = { pomModel ->
    def bootPlugin = pomModel.build.plugins.plugin.find {
        it.artifactId.text() == "spring-boot-maven-plugin"
    }
    assert bootPlugin.artifactId.text() == "spring-boot-maven-plugin": "Expected spring-boot-maven-plugin"
    assert bootPlugin.configuration.layers.enabled.text() == "true"
    assert bootPlugin.configuration.excludes.exclude.any {
        it.groupId.text() == "org.projectlombok" && it.artifactId.text() == "lombok"
    }: "Expected spring-boot-maven-plugin to exclude org.projectlombok:lombok"
}

def moduleArtifactIds = [
    "student-management-organization-common",
    "student-management-organization-facade",
    "student-management-organization-domain",
    "student-management-organization-application",
    "student-management-organization-infrastructure",
    "student-management-organization-adapter",
    "student-management-organization-starter"
] as Set

def moduleDependencies = { deps ->
    deps.findAll { moduleArtifactIds.contains(it.artifactId) }
            .collect { it.artifactId } as Set
}

def assertModuleDependencies = { deps, expected ->
    def actual = moduleDependencies(deps)
    assert actual == (expected as Set): "Expected module dependencies ${expected}, but got ${actual}"
}

def commonPom = modulePom("common")
def facadePom = modulePom("facade")
def domainPom = modulePom("domain")
def applicationPom = modulePom("application")
def infrastructurePom = modulePom("infrastructure")
def adapterPom = modulePom("adapter")
def starterPom = modulePom("starter")
def starterPomText = assertFile("student-management-organization-starter/pom.xml").text
assert starterPomText.contains("<artifactId>spring-boot-maven-plugin</artifactId>")
assert starterPomText.contains("<layers>")
assert starterPomText.contains("<enabled>true</enabled>")
assertSpringBootLayeredJarPlugin(starterPom)

assert commonPom.artifactId.text() == "student-management-organization-common"
assert facadePom.artifactId.text() == "student-management-organization-facade"
assert domainPom.artifactId.text() == "student-management-organization-domain"
assert applicationPom.artifactId.text() == "student-management-organization-application"
assert infrastructurePom.artifactId.text() == "student-management-organization-infrastructure"
assert adapterPom.artifactId.text() == "student-management-organization-adapter"
assert starterPom.artifactId.text() == "student-management-organization-starter"

def facadeDependencies = dependencies(facadePom)
def domainDependencies = dependencies(domainPom)
def applicationDependencies = dependencies(applicationPom)
def infrastructureDependencies = dependencies(infrastructurePom)
def adapterDependencies = dependencies(adapterPom)
def starterDependencies = dependencies(starterPom)

def externalFacadeDependencies = { pomModel ->
    pomModel.dependencies.dependency.findAll {
        it.groupId.text() == '${evaluation-facade.group-id}'
                || it.artifactId.text() == '${evaluation-facade.artifact-id}'
    }.collect { [groupId: it.groupId.text(), artifactId: it.artifactId.text()] }
}
assert externalFacadeDependencies(infrastructurePom) == [[
    groupId: '${evaluation-facade.group-id}',
    artifactId: '${evaluation-facade.artifact-id}'
]]
[
    common: commonPom,
    facade: facadePom,
    domain: domainPom,
    application: applicationPom,
    adapter: adapterPom,
    starter: starterPom
].each { module, pomModel ->
    assert externalFacadeDependencies(pomModel).isEmpty():
            "Unexpected Evaluation Facade dependency in ${module}"
}

def assertExactExternalDependencies = { module, actual, expected ->
    def external = actual.findAll { !it.artifactId.startsWith("student-management-organization-") }
            .collect { it.artifactId } as Set
    assert external == expected as Set:
            "Unexpected external dependencies for ${module}: expected ${expected.sort()}, got ${external.sort()}"
}

assertModuleDependencies(dependencies(commonPom), [])
assertModuleDependencies(facadeDependencies, [])
assertModuleDependencies(domainDependencies, ["student-management-organization-common"])
assertModuleDependencies(applicationDependencies, ["student-management-organization-domain"])
assertModuleDependencies(infrastructureDependencies, ["student-management-organization-domain"])
assertModuleDependencies(adapterDependencies, [
    "student-management-organization-application",
    "student-management-organization-facade"
])
assertModuleDependencies(starterDependencies, [
    "student-management-organization-adapter",
    "student-management-organization-infrastructure"
])

assertExactExternalDependencies("common", dependencies(commonPom), [
    "egon-cola-component-common-core", "junit-jupiter"
])
assertExactExternalDependencies("facade", facadeDependencies, [
    "jakarta.validation-api", "lombok", "junit-jupiter", "hibernate-validator", "jackson-databind"
])
assertExactExternalDependencies("domain", domainDependencies, ["junit-jupiter"])
assertExactExternalDependencies("application", applicationDependencies, [
    "spring-context", "spring-tx", "jakarta.validation-api", "mapstruct-plus-spring-boot-starter",
    "lombok", "junit-jupiter", "mockito-junit-jupiter"
])
assertExactExternalDependencies("infrastructure", infrastructureDependencies, [
    '${evaluation-facade.artifact-id}', "dubbo-spring-boot-starter",
    "spring-boot-starter-data-jpa", "spring-boot-starter-data-redis", "spring-boot-starter-amqp",
    "spring-boot-starter-aop", "micrometer-core", "flyway-core", "h2", "postgresql",
    "mapstruct-plus-spring-boot-starter", "lombok", "spring-boot-starter-test"
])
assertExactExternalDependencies("adapter", adapterDependencies, [
    "spring-boot-starter-web", "spring-boot-starter-validation", "spring-boot-starter-graphql",
    "spring-boot-starter-amqp", "springdoc-openapi-starter-webmvc-api", "dubbo-spring-boot-starter",
    "mapstruct-plus-spring-boot-starter", "lombok", "spring-boot-starter-test", "spring-graphql-test"
])
assertExactExternalDependencies("starter", starterDependencies, [
    "spring-boot-starter", "spring-boot-starter-actuator", "springdoc-openapi-starter-webmvc-ui",
    "spring-cloud-starter-bootstrap", "spring-cloud-starter-alibaba-nacos-discovery",
    "spring-cloud-starter-alibaba-nacos-config", "micrometer-registry-prometheus", "lombok",
    "spring-boot-starter-test", "archunit-junit5"
])

assertDependency(facadeDependencies, "jakarta.validation-api")
assertNoDependency(facadeDependencies, "spring-boot-starter-validation")
assertScopedDependency(facadeDependencies, "lombok", "provided")

assertDependency(domainDependencies, "student-management-organization-common")
assertNoDependency(domainDependencies, "spring-boot-starter-validation")

assertDependency(applicationDependencies, "student-management-organization-domain")
assertDependency(applicationDependencies, "spring-context")
assertDependency(applicationDependencies, "spring-tx")
assertDependency(applicationDependencies, "jakarta.validation-api")
assertDependency(applicationDependencies, "mapstruct-plus-spring-boot-starter")
assertNoDependency(applicationDependencies, "spring-boot-starter-validation")
assertScopedDependency(applicationDependencies, "lombok", "provided")
assertNoDependency(applicationDependencies, "student-management-organization-common")
assertNoDependency(applicationDependencies, "student-management-organization-infrastructure")

assertDependency(infrastructureDependencies, "student-management-organization-domain")
assertDependency(infrastructureDependencies, '${evaluation-facade.artifact-id}')
assertDependency(infrastructureDependencies, "dubbo-spring-boot-starter")
assertNoDependency(infrastructureDependencies, "student-management-organization-common")
assertNoDependency(infrastructureDependencies, "spring-boot-starter-validation")
assertDependency(infrastructureDependencies, "micrometer-core")
assertDependency(infrastructureDependencies, "mapstruct-plus-spring-boot-starter")
assertDependency(infrastructureDependencies, "spring-boot-starter-data-jpa")
assertDependency(infrastructureDependencies, "spring-boot-starter-data-redis")
assertDependency(infrastructureDependencies, "spring-boot-starter-amqp")
assertDependency(infrastructureDependencies, "spring-boot-starter-aop")
assertDependency(infrastructureDependencies, "flyway-core")
assertScopedDependency(infrastructureDependencies, "h2", "runtime")
assertScopedDependency(infrastructureDependencies, "postgresql", "runtime")
assertScopedDependency(infrastructureDependencies, "lombok", "provided")

assertDependency(adapterDependencies, "student-management-organization-application")
assertDependency(adapterDependencies, "student-management-organization-facade")
assertNoDependency(adapterDependencies, "student-management-organization-common")
assertDependency(adapterDependencies, "spring-boot-starter-web")
assertDependency(adapterDependencies, "spring-boot-starter-validation")
assertDependency(adapterDependencies, "spring-boot-starter-graphql")
assertDependency(adapterDependencies, "spring-boot-starter-amqp")
assertDependency(adapterDependencies, "springdoc-openapi-starter-webmvc-api")
assertDependency(adapterDependencies, "dubbo-spring-boot-starter")
assertDependency(adapterDependencies, "mapstruct-plus-spring-boot-starter")
assertScopedDependency(adapterDependencies, "lombok", "provided")
assertNoDependency(adapterDependencies, "student-management-organization-infrastructure")

assertDependency(starterDependencies, "student-management-organization-adapter")
assertDependency(starterDependencies, "student-management-organization-infrastructure")
assertDependency(starterDependencies, "spring-boot-starter")
assertDependency(starterDependencies, "spring-boot-starter-actuator")
assertDependency(starterDependencies, "springdoc-openapi-starter-webmvc-ui")
assertScopedDependency(starterDependencies, "lombok", "provided")
assertScopedDependency(starterDependencies, "spring-boot-starter-test", "test")
assertScopedDependency(starterDependencies, "archunit-junit5", "test")
assert starterPomText.contains("<artifactId>spring-cloud-starter-bootstrap</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>")
assert starterPomText.contains("<artifactId>micrometer-registry-prometheus</artifactId>")

assertMissing("student-management-organization-common/src/main/java/it/pkg/common/response/Response.java")
assertMissing("student-management-organization-common/src/main/java/it/pkg/common/response/SingleResponse.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/UserFacade.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/RoleFacade.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/PermissionFacade.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/teaching/SchoolClassFacade.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/teaching/GradeFacade.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/user/entities/User.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/user/aggregates/UserAggregate.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/user/aggregates/RolePermissionAggregate.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/teaching/entities/SchoolClass.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/teaching/entities/Grade.java")
assertMissing("student-management-organization-domain/src/main/java/it/pkg/domain/user/client/UserClient.java")
assertMissing("student-management-organization-domain/src/main/java/it/pkg/domain/teaching/client/SchoolClassClient.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/user/manage/UserManage.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/user/manage/RoleManage.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/user/manage/PermissionManage.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/SchoolClassManage.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/GradeManage.java")
assertMissing("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/user/UserClientImpl.java")
assertMissing("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/impl/teaching/SchoolClassClientImpl.java")
assertFile("student-management-organization-infrastructure/src/main/resources/db/migration/V1__init_student_management_organization.sql")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/user/controller/UserController.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/user/controller/RoleController.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/user/controller/PermissionController.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/controller/SchoolClassController.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/controller/GradeController.java")
assertMissing("student-management-organization-adapter/src/main/java/it/pkg/adapter/validation/ValidatorUtils.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/handler/OrganizationGlobalExceptionHandler.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/filter/OrganizationTraceFilter.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/filter/OrganizationAuthContextFilter.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/OrganizationApplication.java")
def asyncConfigurationText = assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/async/AsyncConfiguration.java").text
assert asyncConfigurationText.contains("implements AsyncConfigurer")
assert asyncConfigurationText.contains("getAsyncUncaughtExceptionHandler")
assert !asyncConfigurationText.contains("ThreadPoolTaskExecutorBuilder")
assert !asyncConfigurationText.contains("applicationTaskExecutor(")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptor.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptException.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptKeyProvider.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptor.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigCipherCli.java")
def springFactories = assertFile("student-management-organization-starter/src/main/resources/META-INF/spring.factories").text
assert springFactories.contains("org.springframework.boot.env.EnvironmentPostProcessor")
assert springFactories.contains("it.pkg.starter.config.encryption.ConfigDecryptEnvironmentPostProcessor")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptorTest.java")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/OrganizationFlowTest.java").text.contains('properties = "spring.profiles.active=test"')
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/ArchitectureDependencyTest.java")

def organizationApplicationText = assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/OrganizationApplication.java").text
assert organizationApplicationText.contains("@EnableDubbo")
assert organizationApplicationText.contains('"it.pkg.adapter.user.rpc"')
assert organizationApplicationText.contains('"it.pkg.adapter.teaching.rpc"')
assert !organizationApplicationText.contains('"it.pkg.adapter.facade"')

assertFile("student-management-organization-application/src/main/java/it/pkg/application/config/DomainServiceConfiguration.java")
assertMissing("student-management-organization-application/src/main/java/it/pkg/application/user/manage/UserView.java")
assertMissing("student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/SchoolClassView.java")

def userManageText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/user/manage/UserManage.java").text
assert userManageText.contains("UserDetailResult createUser(CreateUserCommand command)")
assert userManageText.contains("UserDetailResult getUser(UserDetailQuery query)")
assert !userManageText.contains("UserView")

def schoolClassManageText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/SchoolClassManage.java").text
assert schoolClassManageText.contains("SchoolClassDetailResult createSchoolClass(CreateSchoolClassCommand command)")
assert schoolClassManageText.contains("SchoolClassDetailResult getSchoolClass(SchoolClassDetailQuery query)")
assert !schoolClassManageText.contains("SchoolClassView")

def userManageImplText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/user/manage/impl/UserManageImpl.java").text
assert userManageImplText.contains("UserRepository userRepository")
assert userManageImplText.contains("userRepository.existsByEmail(normalizedEmail)")
assert userManageImplText.contains("UserDetailResult createUser")
assert !userManageImplText.contains("UserClient")

def schoolClassManageImplText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/teaching/manage/impl/SchoolClassManageImpl.java").text
assert schoolClassManageImplText.contains("SchoolClassRepository schoolClassRepository")
assert schoolClassManageImplText.contains("GradeRepository gradeRepository")
assert schoolClassManageImplText.contains("existsByGradeIdAndNameIgnoreCase")
assert !schoolClassManageImplText.contains("SchoolClassClient")

def userFacadeText = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/user/facade/impl/UserFacadeImpl.java").text
assert userFacadeText.contains("implements UserFacade")
assert userFacadeText.contains("UserDetailDTO createUser")
assert !userFacadeText.contains("@DubboService")

def schoolClassFacadeText = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/facade/impl/SchoolClassFacadeImpl.java").text
assert schoolClassFacadeText.contains("implements SchoolClassFacade")
assert schoolClassFacadeText.contains("SchoolClassDetailDTO createSchoolClass")
assert !schoolClassFacadeText.contains("@DubboService")

assertNoGenericMapStructConverterInjection("student-management-organization-adapter/src/main/java/it/pkg/adapter/user/converter")
assertNoGenericMapStructConverterInjection("student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/converter")
assertNoGenericMapStructConverterInjection("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo")
assertNoGenericMapStructConverterInjection("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo")

assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/user/converter/UserAdapterConverter.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/teaching/converter/SchoolClassAdapterConverter.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/converter/UserPOConverter.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/UserPO.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/RolePO.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/PermissionPO.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/UserRolePO.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/user/repo/po/RolePermissionPO.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/converter/SchoolClassPOConverter.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/converter/GradePOConverter.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/po/SchoolClassPO.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/teaching/command/AssignUserToClassCommand.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/teaching/aggregates/SchoolClassAggregate.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/po/SchoolClassUserPO.java")
assertMissing("student-management-organization-domain/src/main/java/it/pkg/domain/common")
assertMissing("student-management-organization-facade/src/main/java/it/pkg/facade/dto/PageResponse.java")

assertMissing("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/SchoolClassAdapterMapper.java")
assertMissing("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/teaching/repo/converter/SchoolClassPoMapper.java")

def userRepositoryText = assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/user/repos/UserRepository.java").text
assert !userRepositoryText.contains("findPage")
assert !userRepositoryText.contains("domain.common.Page")

def userControllerText = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/user/controller/UserController.java").text
assert userControllerText.contains('@RequestMapping("/api/v1/users")')
assert userControllerText.contains("ResponseEntity<UserDetailVO> create")

def userFacadeContractText = assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/UserFacade.java").text
assert userFacadeContractText.contains("UserDetailDTO createUser")
assert userFacadeContractText.contains("UserDetailDTO getUser")

def applicationJava = []
new File(projectDir, "student-management-organization-application/src/main/java").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        applicationJava << file
    }
}
assert applicationJava.every { !it.text.contains("View") }
assert applicationJava.every { !it.text.contains("facade.dto") }
assert applicationJava.every { !it.text.contains("common.response") }

def facadeJava = []
new File(projectDir, "student-management-organization-facade/src/main/java").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        facadeJava << file
    }
}
assert facadeJava.every { !it.text.contains("import it.pkg.domain.") }
assert facadeJava.every { !it.text.contains("@AutoMapper") }
assert facadeJava.every { !it.text.contains("@Component") }

def migrationDir = new File(projectDir, "student-management-organization-infrastructure/src/main/resources/db/migration")
def migrationFiles = migrationDir.listFiles({ dir, name -> name.endsWith(".sql") } as FilenameFilter)
assert migrationFiles.size() == 2
def sha256 = { File file ->
    java.security.MessageDigest.getInstance("SHA-256").digest(file.bytes).encodeHex().toString()
}
assert sha256(assertFile("student-management-organization-infrastructure/src/main/resources/db/migration/V1__init_student_management_organization.sql")) ==
    "c5481736a3ffefc45197a767aec26c1462bb338dfccc1d11751a782ac3de6df1"
assertFile("student-management-organization-infrastructure/src/main/resources/db/migration/V2__complete_organization_domains.sql")
assert migrationFiles.findAll { it.name.startsWith("V2__") }.size() == 1
assertMissing("student-management-organization-infrastructure/src/test/resources/db/migration")

def readme = assertFile("README.md").text
assert readme.contains("Student Management Organization")
assert readme.contains("organization-only Project")
assert readme.contains("user")
assert readme.contains("teaching")
assert readme.contains("clean verify")
assert !readme.contains("\n./mvnw ")
assert readme.contains("Evaluation Facade")
[
    "facade/user/dto",
    "domain/user/entities",
    "application/teaching/manage",
    "infrastructure/user/repo",
    "adapter/teaching/controller"
].each { assert readme.contains(it) }
assert !readme.contains("adapter/controller/user")
assert !readme.contains("application/manage/teaching")

def scannedFiles = []
collectSourceConfigDocFiles(projectDir, scannedFiles)
def generatedJavaFiles = scannedFiles.findAll { it.name.endsWith(".java") }
def forbiddenWebPathFragments = [
    "/facade/dto/user/", "/facade/dto/teaching/",
    "/domain/aggregates/user/", "/domain/aggregates/teaching/",
    "/domain/client/user/", "/domain/client/teaching/",
    "/domain/entities/user/", "/domain/entities/teaching/",
    "/domain/enums/user/", "/domain/enums/teaching/",
    "/domain/events/user/", "/domain/events/teaching/",
    "/domain/repos/user/", "/domain/repos/teaching/",
    "/domain/service/user/", "/domain/service/teaching/",
    "/domain/validators/user/", "/domain/validators/teaching/",
    "/domain/vos/user/", "/domain/vos/teaching/",
    "/application/command/user/", "/application/command/teaching/",
    "/application/converter/user/", "/application/converter/teaching/",
    "/application/manage/user/", "/application/manage/teaching/",
    "/application/query/user/", "/application/query/teaching/",
    "/application/result/user/", "/application/result/teaching/",
    "/application/validators/user/", "/application/validators/teaching/",
    "/application/assemblers/user/", "/application/assemblers/teaching/",
    "/infrastructure/repo/user/", "/infrastructure/repo/teaching/",
    "/adapter/controller/user/", "/adapter/controller/teaching/",
    "/adapter/dto/user/", "/adapter/dto/teaching/",
    "/adapter/vo/user/", "/adapter/vo/teaching/",
    "/adapter/facade/impl/user/", "/adapter/facade/impl/teaching/",
    "/adapter/mq/user/", "/adapter/mq/teaching/"
]
def staleWebPaths = generatedJavaFiles.collect(relativePath).findAll { path ->
    forbiddenWebPathFragments.any { path.contains(it) }
}
assert staleWebPaths.isEmpty(): "Unexpected technical-first Web paths: ${staleWebPaths.join(', ')}"

def providerImports = generatedJavaFiles.findAll {
    it.getText("UTF-8").contains("import fixture.evaluation.facade.")
}
assert providerImports.every {
    def path = relativePath(it)
    path.startsWith("student-management-organization-infrastructure/src/")
            && path.contains("/infrastructure/client/evaluation/")
}: "Evaluation Facade imports escaped Infrastructure client: ${providerImports.collect(relativePath)}"

def dubboReferenceImports = generatedJavaFiles.findAll {
    it.getText("UTF-8").contains("import org.apache.dubbo.config.annotation.DubboReference;")
}
assert dubboReferenceImports.every {
    relativePath(it).contains("/infrastructure/client/evaluation/")
}: "Dubbo references escaped Infrastructure client: ${dubboReferenceImports.collect(relativePath)}"

def applicationManageFiles = generatedJavaFiles.findAll {
    def path = relativePath(it)
    path.startsWith("student-management-organization-application/src/main/java/")
            && (path.contains("/application/user/manage/")
            || path.contains("/application/teaching/manage/"))
}
assert applicationManageFiles.every {
    !it.getText("UTF-8").contains("EvaluationQueryPort")
}: "EvaluationQueryPort must remain unused by current Application use cases"

def localEvaluationStub = assertFile(
        "student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/client/evaluation/LocalEvaluationQueryStub.java").text
assert !localEvaluationStub.contains("fixture.evaluation")
assert !localEvaluationStub.contains("org.apache.dubbo")

[
    "__rootArtifactId__-client",
    'dir="__rootArtifactId__-app"',
    'name="${rootArtifactId}-app"',
    "student-management-organization-client",
    "<module>student-management-organization-app</module>",
    "<artifactId>student-management-organization-app</artifactId>",
    "<name>student-management-organization-app</name>",
    "<module>start</module>",
    "app1",
    "app2",
    "examing",
    "package it.pkg.customer",
    "package it.pkg.order",
    "import it.pkg.customer",
    "import it.pkg.order",
    "/customer/",
    "/order/"
].each { token ->
    assertNoStaleText(scannedFiles, token)
}

["convertor", "examing"].each { token ->
    def matches = scannedFiles.findAll { it.getText("UTF-8").toLowerCase(Locale.ROOT).contains(token) }
            .collect { relativePath(it) }
    assert matches.isEmpty(): "Unexpected stale token '${token}' in ${matches.join(', ')}"
}

def livingArchitectureDoc
for (def cursor = basedir; cursor != null && livingArchitectureDoc == null; cursor = cursor.parentFile) {
    def candidate = new File(cursor, "multi-project-multi-module-architecture.md")
    if (candidate.isFile()) livingArchitectureDoc = candidate
}
assert livingArchitectureDoc != null: "Expected Web living architecture document"
def livingArchitectureText = livingArchitectureDoc.getText("UTF-8")
def forbiddenLivingArchitecturePatterns = [
    ~/adapter\/facade\.impl/,
    ~/adapter\.facade\.impl/,
    ~/adapter\.(mq|controller|dto|vo)\b/,
    ~/application\.(manage|command|converter|query|result|validators)\.(user|teaching|course|exam)\b/,
    ~/\bmanage\.(user|teaching|course|exam)\.impl\b/,
    ~/domain\.(aggregates|entities|enums|events|repos|service|validators|vos)\.(user|teaching|course|exam)\b/,
    ~/infrastructure\.(repo|mq|cache)\b/,
    ~/\brepo\.(user|teaching|course|exam)\b/
]
forbiddenLivingArchitecturePatterns.each { pattern ->
    assert !pattern.matcher(livingArchitectureText).find():
            "Unexpected technical-first Web living-doc pattern ${pattern}"
}

null
