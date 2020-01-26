package pro.taskana.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.BulkOperationResults;
import pro.taskana.TaskState;
import pro.taskana.TaskanaRole;
import pro.taskana.Workbasket;
import pro.taskana.WorkbasketAccessItem;
import pro.taskana.WorkbasketAccessItemQuery;
import pro.taskana.WorkbasketPermission;
import pro.taskana.WorkbasketQuery;
import pro.taskana.WorkbasketService;
import pro.taskana.WorkbasketSummary;
import pro.taskana.configuration.TaskanaEngineConfiguration;
import pro.taskana.exceptions.DomainNotFoundException;
import pro.taskana.exceptions.InvalidArgumentException;
import pro.taskana.exceptions.InvalidWorkbasketException;
import pro.taskana.exceptions.NotAuthorizedException;
import pro.taskana.exceptions.TaskanaException;
import pro.taskana.exceptions.WorkbasketAccessItemAlreadyExistException;
import pro.taskana.exceptions.WorkbasketAlreadyExistException;
import pro.taskana.exceptions.WorkbasketInUseException;
import pro.taskana.exceptions.WorkbasketNotFoundException;
import pro.taskana.impl.util.IdGenerator;
import pro.taskana.impl.util.LoggerUtils;
import pro.taskana.mappings.DistributionTargetMapper;
import pro.taskana.mappings.WorkbasketAccessMapper;
import pro.taskana.mappings.WorkbasketMapper;
import pro.taskana.security.CurrentUserContext;

/** This is the implementation of WorkbasketService. */
public class WorkbasketServiceImpl implements WorkbasketService {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkbasketServiceImpl.class);
  private static final String ID_PREFIX_WORKBASKET = "WBI";
  private static final String ID_PREFIX_WORKBASKET_AUTHORIZATION = "WAI";
  private InternalTaskanaEngine taskanaEngine;
  private WorkbasketMapper workbasketMapper;
  private DistributionTargetMapper distributionTargetMapper;
  private WorkbasketAccessMapper workbasketAccessMapper;

  WorkbasketServiceImpl(
      InternalTaskanaEngine taskanaEngine,
      WorkbasketMapper workbasketMapper,
      DistributionTargetMapper distributionTargetMapper,
      WorkbasketAccessMapper workbasketAccessMapper) {
    this.taskanaEngine = taskanaEngine;
    this.workbasketMapper = workbasketMapper;
    this.distributionTargetMapper = distributionTargetMapper;
    this.workbasketAccessMapper = workbasketAccessMapper;
  }

  @Override
  public Workbasket getWorkbasket(String workbasketId)
      throws WorkbasketNotFoundException, NotAuthorizedException {
    LOGGER.debug("entry to getWorkbasket(workbasketId = {})", workbasketId);
    Workbasket result = null;
    try {
      taskanaEngine.openConnection();
      result = workbasketMapper.findById(workbasketId);
      if (result == null) {
        throw new WorkbasketNotFoundException(
            workbasketId, "Workbasket with id " + workbasketId + " was not found.");
      }
      if (!taskanaEngine.getEngine().isUserInRole(TaskanaRole.ADMIN, TaskanaRole.BUSINESS_ADMIN)) {
        this.checkAuthorization(workbasketId, WorkbasketPermission.READ);
      }
      return result;
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from getWorkbasket(workbasketId). Returning result {} ", result);
    }
  }

  @Override
  public Workbasket getWorkbasket(String workbasketKey, String domain)
      throws WorkbasketNotFoundException, NotAuthorizedException {
    LOGGER.debug("entry to getWorkbasketByKey(workbasketKey = {})", workbasketKey);
    Workbasket result = null;
    try {
      taskanaEngine.openConnection();
      result = workbasketMapper.findByKeyAndDomain(workbasketKey, domain);
      if (result == null) {
        throw new WorkbasketNotFoundException(
            workbasketKey,
            domain,
            "Workbasket with key " + workbasketKey + " and domain " + domain + " was not found.");
      }
      if (!taskanaEngine.getEngine().isUserInRole(TaskanaRole.ADMIN, TaskanaRole.BUSINESS_ADMIN)) {
        this.checkAuthorization(workbasketKey, domain, WorkbasketPermission.READ);
      }
      return result;
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from getWorkbasket(workbasketId). Returning result {} ", result);
    }
  }

  @Override
  public Workbasket createWorkbasket(Workbasket newWorkbasket)
      throws InvalidWorkbasketException, NotAuthorizedException, WorkbasketAlreadyExistException,
          DomainNotFoundException {
    LOGGER.debug("entry to createtWorkbasket(workbasket)", newWorkbasket);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);

    WorkbasketImpl workbasket = (WorkbasketImpl) newWorkbasket;
    try {
      taskanaEngine.openConnection();
      Instant now = Instant.now();
      workbasket.setCreated(now);
      workbasket.setModified(now);
      Workbasket existingWorkbasket =
          workbasketMapper.findByKeyAndDomain(newWorkbasket.getKey(), newWorkbasket.getDomain());
      if (existingWorkbasket != null) {
        throw new WorkbasketAlreadyExistException(existingWorkbasket);
      }

      if (workbasket.getId() == null || workbasket.getId().isEmpty()) {
        workbasket.setId(IdGenerator.generateWithPrefix(ID_PREFIX_WORKBASKET));
      }
      validateWorkbasket(workbasket);

      workbasketMapper.insert(workbasket);
      LOGGER.debug("Method createWorkbasket() created Workbasket '{}'", workbasket);
      return workbasket;
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from createWorkbasket(workbasket). Returning result {} ", workbasket);
    }
  }

  @Override
  public Workbasket updateWorkbasket(Workbasket workbasketToUpdate) throws NotAuthorizedException {
    LOGGER.debug("entry to updateWorkbasket(workbasket)", workbasketToUpdate);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);

    WorkbasketImpl workbasket = (WorkbasketImpl) workbasketToUpdate;
    try {
      taskanaEngine.openConnection();
      workbasket.setModified(Instant.now());
      if (workbasket.getId() == null || workbasket.getId().isEmpty()) {
        workbasketMapper.updateByKeyAndDomain(workbasket);
      } else {
        workbasketMapper.update(workbasket);
      }
      LOGGER.debug("Method updateWorkbasket() updated workbasket '{}'", workbasket.getId());
      return workbasket;
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from updateWorkbasket(). Returning result {} ", workbasket);
    }
  }

  @Override
  public WorkbasketAccessItem newWorkbasketAccessItem(String workbasketId, String accessId) {
    WorkbasketAccessItemImpl accessItem = new WorkbasketAccessItemImpl();
    accessItem.setWorkbasketId(workbasketId);
    if (TaskanaEngineConfiguration.shouldUseLowerCaseForAccessIds()) {
      accessItem.setAccessId(accessId != null ? accessId.toLowerCase() : null);
    } else {
      accessItem.setAccessId(accessId);
    }
    return accessItem;
  }

  @Override
  public WorkbasketAccessItem createWorkbasketAccessItem(WorkbasketAccessItem workbasketAccessItem)
      throws InvalidArgumentException, NotAuthorizedException, WorkbasketNotFoundException,
          WorkbasketAccessItemAlreadyExistException {
    LOGGER.debug(
        "entry to createWorkbasketAccessItemn(workbasketAccessItem = {})", workbasketAccessItem);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);
    WorkbasketAccessItemImpl accessItem = (WorkbasketAccessItemImpl) workbasketAccessItem;
    try {
      taskanaEngine.openConnection();
      accessItem.setId(IdGenerator.generateWithPrefix(ID_PREFIX_WORKBASKET_AUTHORIZATION));
      if (workbasketAccessItem.getId() == null
          || workbasketAccessItem.getAccessId() == null
          || workbasketAccessItem.getWorkbasketId() == null) {
        throw new InvalidArgumentException(
            String.format(
                "Checking the preconditions of the current "
                    + "WorkbasketAccessItem failed. WorkbasketAccessItem=%s",
                workbasketAccessItem));
      }
      WorkbasketImpl wb = workbasketMapper.findById(workbasketAccessItem.getWorkbasketId());
      if (wb == null) {
        throw new WorkbasketNotFoundException(
            workbasketAccessItem.getWorkbasketId(),
            String.format(
                "WorkbasketAccessItem %s refers to a not existing workbasket",
                workbasketAccessItem));
      }
      boolean accessIdAlreadyExists =
          getWorkbasketAccessItems(workbasketAccessItem.getWorkbasketId()).stream()
              .map(WorkbasketAccessItem::getAccessId)
              .anyMatch(i -> i.equals(workbasketAccessItem.getAccessId()));
      if (accessIdAlreadyExists) {
        throw new WorkbasketAccessItemAlreadyExistException(accessItem);
      }
      workbasketAccessMapper.insert(accessItem);
      LOGGER.debug(
          "Method createWorkbasketAccessItem() created workbaskteAccessItem {}", accessItem);
      return accessItem;
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug(
          "exit from createWorkbasketAccessItem(workbasketAccessItem). Returning result {}",
          accessItem);
    }
  }

  @Override
  public WorkbasketAccessItem updateWorkbasketAccessItem(WorkbasketAccessItem workbasketAccessItem)
      throws InvalidArgumentException, NotAuthorizedException {
    LOGGER.debug(
        "entry to updateWorkbasketAccessItem(workbasketAccessItem = {}", workbasketAccessItem);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);
    WorkbasketAccessItemImpl accessItem = (WorkbasketAccessItemImpl) workbasketAccessItem;
    try {
      taskanaEngine.openConnection();
      WorkbasketAccessItem originalItem = workbasketAccessMapper.findById(accessItem.getId());

      if ((originalItem.getAccessId() != null
              && !originalItem.getAccessId().equals(accessItem.getAccessId()))
          || (originalItem.getWorkbasketId() != null
              && !originalItem.getWorkbasketId().equals(accessItem.getWorkbasketId()))) {
        throw new InvalidArgumentException(
            "AccessId and WorkbasketId must not be changed in updateWorkbasketAccessItem calls");
      }

      workbasketAccessMapper.update(accessItem);
      LOGGER.debug(
          "Method updateWorkbasketAccessItem() updated workbasketAccessItem {}", accessItem);
      return accessItem;
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug(
          "exit from updateWorkbasketAccessItem(workbasketAccessItem). Returning {}", accessItem);
    }
  }

  @Override
  public void deleteWorkbasketAccessItem(String accessItemId) throws NotAuthorizedException {
    LOGGER.debug("entry to deleteWorkbasketAccessItem(id = {})", accessItemId);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);
    try {
      taskanaEngine.openConnection();
      workbasketAccessMapper.delete(accessItemId);
      LOGGER.debug(
          "Method deleteWorkbasketAccessItem() deleted workbasketAccessItem wit Id {}",
          accessItemId);
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from deleteWorkbasketAccessItem(id).");
    }
  }

  @Override
  public void checkAuthorization(String workbasketId, WorkbasketPermission... requestedPermissions)
      throws NotAuthorizedException, WorkbasketNotFoundException {
    boolean isAuthorized = true;
    try {
      taskanaEngine.openConnection();

      if (workbasketMapper.findById(workbasketId) == null) {
        throw new WorkbasketNotFoundException(
            workbasketId, "Workbasket with id " + workbasketId + " was not found.");
      }

      if (skipAuthorizationCheck()) {
        return;
      }

      List<String> accessIds = CurrentUserContext.getAccessIds();
      WorkbasketAccessItem wbAcc =
          workbasketAccessMapper.findByWorkbasketAndAccessId(workbasketId, accessIds);
      if (wbAcc == null) {
        throw new NotAuthorizedException(
            "Not authorized. Permission '"
                + Arrays.toString(requestedPermissions)
                + "' on workbasket '"
                + workbasketId
                + "' is needed.",
            CurrentUserContext.getUserid());
      }

      List<WorkbasketPermission> grantedPermissions =
          this.getPermissionsFromWorkbasketAccessItem(wbAcc);

      for (WorkbasketPermission perm : requestedPermissions) {
        if (!grantedPermissions.contains(perm)) {
          isAuthorized = false;
          throw new NotAuthorizedException(
              "Not authorized. Permission '"
                  + perm.name()
                  + "' on workbasket '"
                  + workbasketId
                  + "' is needed.",
              CurrentUserContext.getUserid());
        }
      }
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from checkAuthorization(). User is authorized = {}.", isAuthorized);
    }
  }

  @Override
  public void checkAuthorization(
      String workbasketKey, String domain, WorkbasketPermission... requestedPermissions)
      throws NotAuthorizedException, WorkbasketNotFoundException {
    boolean isAuthorized = true;
    try {
      taskanaEngine.openConnection();

      if (workbasketMapper.findByKeyAndDomain(workbasketKey, domain) == null) {
        throw new WorkbasketNotFoundException(
            workbasketKey,
            domain,
            "Workbasket with key " + workbasketKey + " and domain " + domain + " was not found");
      }
      if (skipAuthorizationCheck()) {
        return;
      }
      List<String> accessIds = CurrentUserContext.getAccessIds();
      WorkbasketAccessItem wbAcc =
          workbasketAccessMapper.findByWorkbasketKeyDomainAndAccessId(
              workbasketKey, domain, accessIds);
      if (wbAcc == null) {
        throw new NotAuthorizedException(
            "Not authorized. Permission '"
                + Arrays.toString(requestedPermissions)
                + "' on workbasket with key '"
                + workbasketKey
                + "' and domain '"
                + domain
                + "' is needed.",
            CurrentUserContext.getUserid());
      }
      List<WorkbasketPermission> grantedPermissions =
          this.getPermissionsFromWorkbasketAccessItem(wbAcc);

      for (WorkbasketPermission perm : requestedPermissions) {
        if (!grantedPermissions.contains(perm)) {
          isAuthorized = false;
          throw new NotAuthorizedException(
              "Not authorized. Permission '"
                  + perm.name()
                  + "' on workbasket with key '"
                  + workbasketKey
                  + "' and domain '"
                  + domain
                  + "' is needed.",
              CurrentUserContext.getUserid());
        }
      }
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from checkAuthorization(). User is authorized = {}.", isAuthorized);
    }
  }

  @Override
  public List<WorkbasketAccessItem> getWorkbasketAccessItems(String workbasketId)
      throws NotAuthorizedException {
    LOGGER.debug("entry to getWorkbasketAccessItems(workbasketId = {})", workbasketId);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);
    List<WorkbasketAccessItem> result = new ArrayList<>();
    try {
      taskanaEngine.openConnection();
      List<WorkbasketAccessItemImpl> queryResult =
          workbasketAccessMapper.findByWorkbasketId(workbasketId);
      result.addAll(queryResult);
      return result;
    } finally {
      taskanaEngine.returnConnection();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "exit from getWorkbasketAccessItems(workbasketId). Returning {} resulting Objects: {} ",
            result.size(),
            LoggerUtils.listToString(result));
      }
    }
  }

  @Override
  public void setWorkbasketAccessItems(
      String workbasketId, List<WorkbasketAccessItem> wbAccessItems)
      throws InvalidArgumentException, NotAuthorizedException,
          WorkbasketAccessItemAlreadyExistException {
    LOGGER.debug(
        "entry to setWorkbasketAccessItems(workbasketAccessItems = {})", wbAccessItems.toString());
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);
    try {
      taskanaEngine.openConnection();
      // delete all current ones
      workbasketAccessMapper.deleteAllAccessItemsForWorkbasketId(workbasketId);

      Set<String> ids = new HashSet<>();
      for (WorkbasketAccessItem workbasketAccessItem : wbAccessItems) {
        WorkbasketAccessItemImpl wbAccessItemImpl = (WorkbasketAccessItemImpl) workbasketAccessItem;
        // Check pre-conditions and set ID
        if (wbAccessItemImpl.getWorkbasketId() == null) {
          throw new InvalidArgumentException(
              String.format(
                  "Checking the preconditions of the current WorkbasketAccessItem failed "
                      + "- WBID is NULL. WorkbasketAccessItem=%s",
                  workbasketAccessItem));
        } else if (!wbAccessItemImpl.getWorkbasketId().equals(workbasketId)) {
          throw new InvalidArgumentException(
              String.format(
                  "Checking the preconditions of the current WorkbasketAccessItem failed "
                      + "- the WBID does not match. Target-WBID=''%s'' WorkbasketAccessItem=%s",
                  workbasketId, workbasketAccessItem));
        }
        if (wbAccessItemImpl.getId() == null || wbAccessItemImpl.getId().isEmpty()) {
          wbAccessItemImpl.setId(
              IdGenerator.generateWithPrefix(ID_PREFIX_WORKBASKET_AUTHORIZATION));
        }
        if (ids.contains(wbAccessItemImpl.getAccessId())) {
          throw new WorkbasketAccessItemAlreadyExistException(wbAccessItemImpl);
        }
        ids.add(wbAccessItemImpl.getAccessId());
        workbasketAccessMapper.insert(wbAccessItemImpl);
      }

    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from setWorkbasketAccessItems(workbasketAccessItems = {})", wbAccessItems);
    }
  }

  @Override
  public WorkbasketQuery createWorkbasketQuery() {
    return new WorkbasketQueryImpl(taskanaEngine);
  }

  @Override
  public WorkbasketAccessItemQuery createWorkbasketAccessItemQuery() throws NotAuthorizedException {
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.ADMIN, TaskanaRole.BUSINESS_ADMIN);
    return new WorkbasketAccessItemQueryImpl(this.taskanaEngine);
  }

  @Override
  public Workbasket newWorkbasket(String key, String domain) {
    WorkbasketImpl wb = new WorkbasketImpl();
    wb.setDomain(domain);
    wb.setKey(key);
    return wb;
  }

  @Override
  public List<WorkbasketPermission> getPermissionsForWorkbasket(String workbasketId) {
    WorkbasketAccessItem wbAcc =
        workbasketAccessMapper.findByWorkbasketAndAccessId(
            workbasketId, CurrentUserContext.getAccessIds());
    return this.getPermissionsFromWorkbasketAccessItem(wbAcc);
  }

  @Override
  public List<WorkbasketSummary> getDistributionTargets(String workbasketId)
      throws NotAuthorizedException, WorkbasketNotFoundException {
    LOGGER.debug("entry to getDistributionTargets(workbasketId = {})", workbasketId);
    List<WorkbasketSummary> result = new ArrayList<>();
    try {
      taskanaEngine.openConnection();
      // check that source workbasket exists
      getWorkbasket(workbasketId);
      if (!taskanaEngine.getEngine().isUserInRole(TaskanaRole.ADMIN, TaskanaRole.BUSINESS_ADMIN)) {
        checkAuthorization(workbasketId, WorkbasketPermission.READ);
      }
      List<WorkbasketSummaryImpl> distributionTargets =
          workbasketMapper.findDistributionTargets(workbasketId);
      result.addAll(distributionTargets);
      return result;
    } finally {
      taskanaEngine.returnConnection();
      if (LOGGER.isDebugEnabled()) {
        int numberOfResultObjects = result.size();
        LOGGER.debug(
            "exit from getDistributionTargets(workbasketId). Returning {} resulting Objects: {} ",
            numberOfResultObjects,
            LoggerUtils.listToString(result));
      }
    }
  }

  @Override
  public List<WorkbasketSummary> getDistributionTargets(String workbasketKey, String domain)
      throws NotAuthorizedException, WorkbasketNotFoundException {
    LOGGER.debug(
        "entry to getDistributionTargets(workbasketKey = {}, domain = {})", workbasketKey, domain);
    List<WorkbasketSummary> result = new ArrayList<>();
    try {
      taskanaEngine.openConnection();
      // check that source workbasket exists
      Workbasket workbasket = getWorkbasket(workbasketKey, domain);
      if (!taskanaEngine.getEngine().isUserInRole(TaskanaRole.ADMIN, TaskanaRole.BUSINESS_ADMIN)) {
        checkAuthorization(workbasket.getId(), WorkbasketPermission.READ);
      }
      List<WorkbasketSummaryImpl> distributionTargets =
          workbasketMapper.findDistributionTargets(workbasket.getId());
      result.addAll(distributionTargets);
      return result;
    } finally {
      taskanaEngine.returnConnection();
      if (LOGGER.isDebugEnabled()) {
        int numberOfResultObjects = result.size();
        LOGGER.debug(
            "exit from getDistributionTargets(workbasketId). Returning {} resulting Objects: {} ",
            numberOfResultObjects,
            LoggerUtils.listToString(result));
      }
    }
  }

  @Override
  public void setDistributionTargets(String sourceWorkbasketId, List<String> targetWorkbasketIds)
      throws WorkbasketNotFoundException, NotAuthorizedException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "entry to setDistributionTargets(sourceWorkbasketId = {}, targetWorkazketIds = {})",
          sourceWorkbasketId,
          LoggerUtils.listToString(targetWorkbasketIds));
    }
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);
    try {
      taskanaEngine.openConnection();
      // check existence of source workbasket
      WorkbasketImpl sourceWorkbasket = (WorkbasketImpl) getWorkbasket(sourceWorkbasketId);
      distributionTargetMapper.deleteAllDistributionTargetsBySourceId(sourceWorkbasketId);

      sourceWorkbasket.setModified(Instant.now());
      workbasketMapper.update(sourceWorkbasket);

      if (targetWorkbasketIds != null) {
        for (String targetId : targetWorkbasketIds) {
          // check for existence of target workbasket
          getWorkbasket(targetId);
          distributionTargetMapper.insert(sourceWorkbasketId, targetId);
          LOGGER.debug(
              "Method setDistributionTargets() created distribution target "
                  + "for source '{}' and target {}",
              sourceWorkbasketId,
              targetId);
        }
      }
    } finally {
      taskanaEngine.returnConnection();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "setDistributionTargets set {} distribution targets to source workbasket {} ",
            targetWorkbasketIds == null ? 0 : targetWorkbasketIds.size(),
            sourceWorkbasketId);
      }
    }
  }

  @Override
  public void addDistributionTarget(String sourceWorkbasketId, String targetWorkbasketId)
      throws NotAuthorizedException, WorkbasketNotFoundException {
    LOGGER.debug(
        "entry to addDistributionTarget(sourceWorkbasketId = {}, targetWorkbasketId = {})",
        sourceWorkbasketId,
        targetWorkbasketId);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);
    try {
      taskanaEngine.openConnection();
      // check existence of source workbasket
      WorkbasketImpl sourceWorkbasket = (WorkbasketImpl) getWorkbasket(sourceWorkbasketId);
      // check existence of target workbasket
      getWorkbasket(targetWorkbasketId);
      // check whether the target is already set as target
      int numOfDistTargets =
          distributionTargetMapper.getNumberOfDistributionTargets(
              sourceWorkbasketId, targetWorkbasketId);
      if (numOfDistTargets > 0) {
        LOGGER.debug(
            "addDistributionTarget detected that the specified "
                + "distribution target exists already. Doing nothing.");
      } else {
        distributionTargetMapper.insert(sourceWorkbasketId, targetWorkbasketId);
        LOGGER.debug(
            "addDistributionTarget inserted distribution target sourceId = {}, targetId = {}",
            sourceWorkbasketId,
            targetWorkbasketId);
        sourceWorkbasket.setModified(Instant.now());
        workbasketMapper.update(sourceWorkbasket);
      }

    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from addDistributionTarget");
    }
  }

  @Override
  public void removeDistributionTarget(String sourceWorkbasketId, String targetWorkbasketId)
      throws NotAuthorizedException {
    LOGGER.debug(
        "entry to removeDistributionTarget(sourceWorkbasketId = {}, targetWorkbasketId = {})",
        sourceWorkbasketId,
        targetWorkbasketId);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);
    try {
      taskanaEngine.openConnection();
      // don't check existence of source / target workbasket to enable cleanup even if the db is
      // corrupted
      // check whether the target is set as target
      int numberOfDistTargets =
          distributionTargetMapper.getNumberOfDistributionTargets(
              sourceWorkbasketId, targetWorkbasketId);
      if (numberOfDistTargets > 0) {
        distributionTargetMapper.delete(sourceWorkbasketId, targetWorkbasketId);
        LOGGER.debug(
            "removeDistributionTarget deleted distribution target sourceId = {}, targetId = {}",
            sourceWorkbasketId,
            targetWorkbasketId);

        try {
          WorkbasketImpl sourceWorkbasket = (WorkbasketImpl) getWorkbasket(sourceWorkbasketId);
          sourceWorkbasket.setModified(Instant.now());
          workbasketMapper.update(sourceWorkbasket);
        } catch (WorkbasketNotFoundException e) {
          LOGGER.debug(
              "removeDistributionTarget found that the source workbasket {} "
                  + "doesn't exist. Ignoring the request... ",
              sourceWorkbasketId);
        }

      } else {
        LOGGER.debug(
            "removeDistributionTarget detected that the specified distribution "
                + "target doesn't exist. Doing nothing...");
      }
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from addDistributionTarget");
    }
  }

  @Override
  public boolean deleteWorkbasket(String workbasketId)
      throws NotAuthorizedException, WorkbasketNotFoundException, WorkbasketInUseException,
          InvalidArgumentException {
    LOGGER.debug("entry to deleteWorkbasket(workbasketId = {})", workbasketId);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);

    validateWorkbasketId(workbasketId);

    try {
      taskanaEngine.openConnection();

      try {
        this.getWorkbasket(workbasketId);
      } catch (WorkbasketNotFoundException ex) {
        LOGGER.debug("Workbasket with workbasketId = {} is already deleted?", workbasketId);
        throw ex;
      }

      long countTasksNotCompletedInWorkbasket =
          taskanaEngine.runAsAdmin(() -> getCountTasksNotCompletedByWorkbasketId(workbasketId));

      if (countTasksNotCompletedInWorkbasket > 0) {
        String errorMessage =
            String.format(
                "Workbasket %s contains %s non-completed tasks and can´t be marked for deletion.",
                workbasketId, countTasksNotCompletedInWorkbasket);
        throw new WorkbasketInUseException(errorMessage);
      }

      long countTasksInWorkbasket =
          taskanaEngine.runAsAdmin(() -> getCountTasksByWorkbasketId(workbasketId));

      boolean canBeDeletedNow = countTasksInWorkbasket == 0;

      if (canBeDeletedNow) {
        workbasketMapper.delete(workbasketId);
        deleteReferencesToWorkbasket(workbasketId);
      } else {
        markWorkbasketForDeletion(workbasketId);
      }
      return canBeDeletedNow;
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from deleteWorkbasket(workbasketId = {})", workbasketId);
    }
  }

  public BulkOperationResults<String, TaskanaException> deleteWorkbaskets(
      List<String> workbasketsIds) throws NotAuthorizedException, InvalidArgumentException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "entry to deleteWorkbaskets(workbasketId = {})",
          LoggerUtils.listToString(workbasketsIds));
    }

    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);

    try {
      taskanaEngine.openConnection();
      if (workbasketsIds == null || workbasketsIds.isEmpty()) {
        throw new InvalidArgumentException("List of WorkbasketIds must not be null.");
      }
      BulkOperationResults<String, TaskanaException> bulkLog = new BulkOperationResults<>();

      Iterator<String> iterator = workbasketsIds.iterator();
      String workbasketIdForDeleting = null;
      while (iterator.hasNext()) {
        try {
          workbasketIdForDeleting = iterator.next();
          if (!deleteWorkbasket(workbasketIdForDeleting)) {
            bulkLog.addError(
                workbasketIdForDeleting,
                new WorkbasketInUseException(
                    "Workbasket with id "
                        + workbasketIdForDeleting
                        + " contains completed tasks not deleted and will not be deleted."));
          }
        } catch (WorkbasketInUseException ex) {
          bulkLog.addError(
              workbasketIdForDeleting,
              new WorkbasketInUseException(
                  "Workbasket with id "
                      + workbasketIdForDeleting
                      + " is in use and will not be deleted."));
        } catch (TaskanaException ex) {
          bulkLog.addError(
              workbasketIdForDeleting,
              new TaskanaException(
                  "Workbasket with id "
                      + workbasketIdForDeleting
                      + " Throw an exception and couldn't be deleted."));
        }
      }
      return bulkLog;
    } finally {
      LOGGER.debug("exit from deleteWorkbaskets()");
      taskanaEngine.returnConnection();
    }
  }

  @Override
  public List<WorkbasketSummary> getDistributionSources(String workbasketId)
      throws NotAuthorizedException, WorkbasketNotFoundException {
    LOGGER.debug("entry to getDistributionSources(workbasketId = {})", workbasketId);
    List<WorkbasketSummary> result = new ArrayList<>();
    try {
      taskanaEngine.openConnection();
      // check that source workbasket exists
      getWorkbasket(workbasketId);
      if (!taskanaEngine.getEngine().isUserInRole(TaskanaRole.ADMIN, TaskanaRole.BUSINESS_ADMIN)) {
        checkAuthorization(workbasketId, WorkbasketPermission.READ);
      }
      List<WorkbasketSummaryImpl> distributionSources =
          workbasketMapper.findDistributionSources(workbasketId);
      result.addAll(distributionSources);
      return result;
    } finally {
      taskanaEngine.returnConnection();
      if (LOGGER.isDebugEnabled()) {
        int numberOfResultObjects = result.size();
        LOGGER.debug(
            "exit from getDistributionSources(workbasketId). Returning {} resulting Objects: {} ",
            numberOfResultObjects,
            LoggerUtils.listToString(result));
      }
    }
  }

  @Override
  public List<WorkbasketSummary> getDistributionSources(String workbasketKey, String domain)
      throws NotAuthorizedException, WorkbasketNotFoundException {
    LOGGER.debug(
        "entry to getDistributionSources(workbasketKey = {}, domain = {})", workbasketKey, domain);
    List<WorkbasketSummary> result = new ArrayList<>();
    try {
      taskanaEngine.openConnection();
      // check that source workbasket exists
      Workbasket workbasket = getWorkbasket(workbasketKey, domain);
      if (!taskanaEngine.getEngine().isUserInRole(TaskanaRole.ADMIN, TaskanaRole.BUSINESS_ADMIN)) {
        checkAuthorization(workbasket.getId(), WorkbasketPermission.READ);
      }
      List<WorkbasketSummaryImpl> distributionSources =
          workbasketMapper.findDistributionSources(workbasket.getId());
      result.addAll(distributionSources);
      return result;
    } finally {
      taskanaEngine.returnConnection();
      if (LOGGER.isDebugEnabled()) {
        int numberOfResultObjects = result.size();
        LOGGER.debug(
            "exit from getDistributionSources(workbasketId). Returning {} resulting Objects: {} ",
            numberOfResultObjects,
            LoggerUtils.listToString(result));
      }
    }
  }

  @Override
  public void deleteWorkbasketAccessItemsForAccessId(String accessId)
      throws NotAuthorizedException {
    LOGGER.debug("entry to deleteWorkbasketAccessItemsForAccessId(accessId = {})", accessId);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);
    try {
      taskanaEngine.openConnection();
      workbasketAccessMapper.deleteAccessItemsForAccessId(accessId);
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from deleteWorkbasketAccessItemsForAccessId(accessId={}).", accessId);
    }
  }

  private void validateWorkbasketId(String workbasketId) throws InvalidArgumentException {
    if (workbasketId == null) {
      throw new InvalidArgumentException("The WorkbasketId can´t be NULL");
    }

    if (workbasketId.isEmpty()) {
      throw new InvalidArgumentException("The WorkbasketId can´t be EMPTY for deleteWorkbasket()");
    }
  }

  private long getCountTasksByWorkbasketId(String workbasketId) {
    return taskanaEngine
        .getEngine()
        .getTaskService()
        .createTaskQuery()
        .workbasketIdIn(workbasketId)
        .count();
  }

  private long getCountTasksNotCompletedByWorkbasketId(String workbasketId) {
    return taskanaEngine
        .getEngine()
        .getTaskService()
        .createTaskQuery()
        .workbasketIdIn(workbasketId)
        .stateNotIn(TaskState.COMPLETED)
        .count();
  }

  private boolean skipAuthorizationCheck() {

    // Skip permission check is security is not enabled
    if (!taskanaEngine.getEngine().getConfiguration().isSecurityEnabled()) {
      LOGGER.debug("Skipping permissions check since security is disabled.");
      return true;
    }

    if (taskanaEngine.getEngine().isUserInRole(TaskanaRole.ADMIN)) {
      LOGGER.debug("Skipping permissions check since user is in role ADMIN.");
      return true;
    }

    return false;
  }

  private void validateWorkbasket(Workbasket workbasket)
      throws InvalidWorkbasketException, DomainNotFoundException {
    // check that required properties (database not null) are set
    if (workbasket.getId() == null || workbasket.getId().length() == 0) {
      throw new InvalidWorkbasketException("Id must not be null for " + workbasket);
    } else if (workbasket.getKey() == null || workbasket.getKey().length() == 0) {
      throw new InvalidWorkbasketException("Key must not be null for " + workbasket);
    }
    if (workbasket.getName() == null || workbasket.getName().length() == 0) {
      throw new InvalidWorkbasketException("Name must not be null for " + workbasket);
    }
    if (workbasket.getDomain() == null) {
      throw new InvalidWorkbasketException("Domain must not be null for " + workbasket);
    }
    if (workbasket.getType() == null) {
      throw new InvalidWorkbasketException("Type must not be null for " + workbasket);
    }
    if (!taskanaEngine.domainExists(workbasket.getDomain())) {
      throw new DomainNotFoundException(
          workbasket.getDomain(),
          "Domain " + workbasket.getDomain() + " does not exist in the configuration.");
    }
  }

  private List<WorkbasketPermission> getPermissionsFromWorkbasketAccessItem(
      WorkbasketAccessItem workbasketAccessItem) {
    List<WorkbasketPermission> permissions = new ArrayList<>();
    if (workbasketAccessItem == null) {
      return permissions;
    }
    if (workbasketAccessItem.isPermOpen()) {
      permissions.add(WorkbasketPermission.OPEN);
    }
    if (workbasketAccessItem.isPermRead()) {
      permissions.add(WorkbasketPermission.READ);
    }
    if (workbasketAccessItem.isPermAppend()) {
      permissions.add(WorkbasketPermission.APPEND);
    }
    if (workbasketAccessItem.isPermTransfer()) {
      permissions.add(WorkbasketPermission.TRANSFER);
    }
    if (workbasketAccessItem.isPermDistribute()) {
      permissions.add(WorkbasketPermission.DISTRIBUTE);
    }
    if (workbasketAccessItem.isPermCustom1()) {
      permissions.add(WorkbasketPermission.CUSTOM_1);
    }
    if (workbasketAccessItem.isPermCustom2()) {
      permissions.add(WorkbasketPermission.CUSTOM_2);
    }
    if (workbasketAccessItem.isPermCustom3()) {
      permissions.add(WorkbasketPermission.CUSTOM_3);
    }
    if (workbasketAccessItem.isPermCustom4()) {
      permissions.add(WorkbasketPermission.CUSTOM_4);
    }
    if (workbasketAccessItem.isPermCustom5()) {
      permissions.add(WorkbasketPermission.CUSTOM_5);
    }
    if (workbasketAccessItem.isPermCustom6()) {
      permissions.add(WorkbasketPermission.CUSTOM_6);
    }
    if (workbasketAccessItem.isPermCustom7()) {
      permissions.add(WorkbasketPermission.CUSTOM_7);
    }
    if (workbasketAccessItem.isPermCustom8()) {
      permissions.add(WorkbasketPermission.CUSTOM_8);
    }
    if (workbasketAccessItem.isPermCustom9()) {
      permissions.add(WorkbasketPermission.CUSTOM_9);
    }
    if (workbasketAccessItem.isPermCustom10()) {
      permissions.add(WorkbasketPermission.CUSTOM_10);
    }
    if (workbasketAccessItem.isPermCustom11()) {
      permissions.add(WorkbasketPermission.CUSTOM_11);
    }
    if (workbasketAccessItem.isPermCustom12()) {
      permissions.add(WorkbasketPermission.CUSTOM_12);
    }
    return permissions;
  }

  private void markWorkbasketForDeletion(String workbasketId)
      throws NotAuthorizedException, InvalidArgumentException {
    LOGGER.debug("entry to markWorkbasketForDeletion(workbasketId = {})", workbasketId);
    taskanaEngine.getEngine().checkRoleMembership(TaskanaRole.BUSINESS_ADMIN, TaskanaRole.ADMIN);
    try {
      taskanaEngine.openConnection();
      validateWorkbasketId(workbasketId);
      WorkbasketImpl workbasket = workbasketMapper.findById(workbasketId);
      workbasket.setMarkedForDeletion(true);
      workbasketMapper.update(workbasket);
    } finally {
      taskanaEngine.returnConnection();
      LOGGER.debug("exit from markWorkbasketForDeletion(workbasketId = {}).", workbasketId);
    }
  }

  private void deleteReferencesToWorkbasket(String workbasketId) {
    // deletes sub-tables workbasket references
    distributionTargetMapper.deleteAllDistributionTargetsBySourceId(workbasketId);
    distributionTargetMapper.deleteAllDistributionTargetsByTargetId(workbasketId);
    workbasketAccessMapper.deleteAllAccessItemsForWorkbasketId(workbasketId);
  }
}
