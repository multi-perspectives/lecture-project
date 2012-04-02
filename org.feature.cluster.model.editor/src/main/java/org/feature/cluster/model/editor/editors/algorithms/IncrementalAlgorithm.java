/**
 * 
 */
package org.feature.cluster.model.editor.editors.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.feature.cluster.model.cluster.CoreGroup;
import org.feature.cluster.model.cluster.Group;
import org.feature.cluster.model.cluster.GroupModel;
import org.feature.cluster.model.cluster.ViewPoint;
import org.feature.cluster.model.cluster.ViewPointContainer;
import org.feature.cluster.model.editor.editors.View;
import org.feature.cluster.model.editor.util.Flag;
import org.feature.cluster.model.editor.util.Util;
import org.featuremapper.models.feature.Feature;
import org.featuremapper.models.feature.FeatureModel;

/**
 * @author Tim Winkelmann
 * 
 */
public class IncrementalAlgorithm {

   private static Logger log = Logger.getLogger(IncrementalAlgorithm.class);
   private HashMap<EObject, View> viewMap;
   private GroupModel groupModel;
   private UsedGroup ugCG;
   private FeatureModel featureModel;
   private final List<View> views;

   // private int i = 0; //used for tests

   /**
    * 
    * @param views
    * @param groupModel
    */
   public IncrementalAlgorithm(List<View> views, GroupModel groupModel, FeatureModel featureModel) {
      this.views = views;
      viewMap = new HashMap<EObject, View>();
      for (View view : views) {
         viewMap.put(view.getGroup(), view);
      }
      this.groupModel = groupModel;
      this.featureModel = featureModel;
   }

   /**
	 * 
	 */
   public List<ViewPointWrapper> checkViewpoints() {
      List<ViewPointWrapper> viewPointConsistency = new ArrayList<ViewPointWrapper>();
      // long time = System.currentTimeMillis();
      ViewPointContainer container = groupModel.getViewPointContainer();
      if (container != null) {
         EList<ViewPoint> viewPoints = container.getViewPoints();
         // log.info("Number of ViewPoints: " + viewPoints.size());
         Set<Group> groups = new HashSet<Group>();
         // get important groups
         for (ViewPoint viewPoint : viewPoints) {
            groups.addAll(viewPoint.getContainedInGroup());
         }
         Map<Group, UsedGroup> usedGroups = createMSGM(groups);
         //
         for (Group g : ugCG.getGroup().getGroups()) {
            if (usedGroups.containsKey(g)) {
               UsedGroup msg = usedGroups.get(g);
               Flag f = new Flag(); // pre-consistency check
               FeatureModel view = Util.createFeatureModel(featureModel, msg.getFeatures(), f);
               if (f.isChanged() && f.isFlagged()) {
                  msg.setConsistent(false);
               } else {
                  msg.setConsistent(Util.isConsistent(view));
               }
               // msg.setConsistent(Util.isConsistent(msg.getFeatures()));
               msg.setDone();
               // i++;//used for tests
               checkGroupModel(g, usedGroups);
            }
         }
         // Display results:
         for (ViewPoint vp : viewPoints) {
            // log.info("Viewpoint: " + vp.getName());
            boolean isCon = true;
            EList<Group> groups2 = vp.getContainedInGroup();
            for (Group group : groups2) {
               UsedGroup usedGroup = usedGroups.get(group);
               boolean consistent = usedGroup.isConsistent();
               // log.info("View: " + group.getName() + ":" + consistent);
               if (isCon && !consistent) {
                  isCon = false;
               }
            }
//            if (!isCon) {
//               // run bruteforce
//               BruteForceAlgorithm bruteForce = new BruteForceAlgorithm(groupModel, views, featureModel);
//               HashMap<EObject, View> viewMemory = new HashMap<EObject, View>();
//               CoreGroup coreGroup = groupModel.getCoreGroup();
//               View view = bruteForce.checkViewpoint(vp, views, coreGroup, viewMemory);
//               isCon = view.isConsistent();
//            }

            ViewPointWrapper wrapper = new ViewPointWrapper(vp.getName(), isCon);
            viewPointConsistency.add(wrapper);
         }
      } else {
         log.info("There are no viewpoints defined yet.");
      }
      return viewPointConsistency;
      // log.debug("Inc: " + (System.currentTimeMillis() - time));
   }

   /**
    * 
    * @param group
    * @param usedGroups
    */
   private void checkGroupModel(Group group, Map<Group, UsedGroup> usedGroups) {
      for (Group g : group.getGroups()) {
         if (usedGroups.containsKey(g)) {
            UsedGroup ug = usedGroups.get(g);
            Flag f = new Flag();
            FeatureModel view = Util.createFeatureModel(featureModel, ug.getFeatures(), f);
            if (f.isChanged() && f.isFlagged()) {
               ug.setConsistent(false);
            } else {
               ug.setConsistent(Util.isConsistent(view));
            }
            // ug.setConsistent(Util.isConsistent(ug.getFeatures()));
            ug.setDone();
            // i++;//used for tests
            checkGroupModel(g, usedGroups);
         }
      }
   }

   /**
    * uses the most specific groups to create a new group model
    * 
    * @param groups most specific groups
    * @return
    */
   private Map<Group, UsedGroup> createMSGM(Set<Group> groups) {
      Map<Group, UsedGroup> usedGroups = new HashMap<Group, UsedGroup>();
      ugCG = new UsedGroup(null, groupModel.getCoreGroup(), viewMap.get(groupModel.getCoreGroup()).getFeatures());
      ugCG.setDone();
      Flag f = new Flag();
      FeatureModel view = Util.createFeatureModel(featureModel, ugCG.getFeatures(), f);
      if (f.isChanged() && f.isFlagged()) {
         ugCG.setConsistent(false);
      } else {
         ugCG.setConsistent(Util.isConsistent(view));
      }
      // ugCG.setConsistent(Util.isConsistent(ugCG.getFeatures()));
      usedGroups.put(groupModel.getCoreGroup(), ugCG);
      for (Group group : groups) {
         if (!usedGroups.containsKey(group)) {
            Set<Feature> features = new HashSet<Feature>();
            features.addAll(viewMap.get(group).getFeatures());

            UsedGroup ugParent = null;
            if (group.getParentGroup() != null) {
               if (group.getParentGroup().equals(groupModel.getCoreGroup())) {
                  ugParent = ugCG;
               } else {
                  ugParent = createMSG((Group) group.getParentGroup(), usedGroups);
               }
               features.addAll(ugParent.getFeatures());
            }
               UsedGroup ug = new UsedGroup(ugParent, group, features);
               usedGroups.put(group, ug);
         }
      }
      return usedGroups;
   }

   /**
    * create a {@link UsedGroup} and his parents (except the ugCG) from the {@link Group} and adds it and his parents to
    * the list.
    * 
    * @param group a most specific group.
    * @param ugs a list of most specific group.
    * @return the most specific group.
    */
   private UsedGroup createMSG(Group group, Map<Group, UsedGroup> ugs) {
      if (ugs.containsKey(group)) {
         return ugs.get(group);
      }
      UsedGroup ugParent;
      if (group.getParentGroup().equals(groupModel.getCoreGroup())) {
         ugParent = ugCG;
      } else {
         ugParent = createMSG((Group) group.getParentGroup(), ugs);
      }

      Set<Feature> features = new HashSet<Feature>();
      features.addAll(ugParent.getFeatures());
      features.addAll(viewMap.get(group).getFeatures());
      UsedGroup ug = new UsedGroup(ugParent, group, features);
      ugs.put(group, ug);
      return ug;
   }
}
