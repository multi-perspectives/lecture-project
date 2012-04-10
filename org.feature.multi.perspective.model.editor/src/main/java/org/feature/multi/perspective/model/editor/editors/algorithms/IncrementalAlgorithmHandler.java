/**
 * 
 */
package org.feature.multi.perspective.model.editor.editors.algorithms;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.feature.model.utilities.FeatureMappingUtil;
import org.feature.model.utilities.WorkbenchUtil;
import org.feature.multi.perspective.model.cluster.ViewPoint;
import org.feature.multi.perspective.model.editor.editors.ClusterMultiPageEditor;
import org.featuremapper.models.featuremapping.FeatureMappingModel;

/**
 * Handler of the Incremental Algorithm
 * 
 * @author Tim Winkelmann
 * 
 */
public class IncrementalAlgorithmHandler extends AbstractHandler {

   @Override
   public Object execute(ExecutionEvent event) throws ExecutionException {

      FeatureMappingModel featureMappingModel = getFeatureMapping();
      if (featureMappingModel != null) {
         IncrementalAlgorithm algorithm = new IncrementalAlgorithm(featureMappingModel);
         List<ViewPoint> viewpoints = algorithm.getInConsistentViewpoints();
         showMessage(viewpoints);
      } else {
         showLoadClusterMsg();
      }
      return null; // No return value needed.
   }

   private void showLoadClusterMsg() {
      Shell shell = WorkbenchUtil.getShell();
      if (shell != null) {
         MessageBox msgBox = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
         String msg = "Cannot validate viewpoints. No Featuremapping loaded yet.";
         msgBox.setMessage(msg);
         msgBox.open();
      }
   }

   private void showMessage(List<ViewPoint> viewpoints) {
      String msg = "";
      int size = viewpoints.size();
      if (0 == size) {
         msg = "All viewpoints are consistent.";
      } else {
         msg += size + " ";
         if (1 == size) {
            msg += "viewpoint is";
         } else {
            msg += "viewpoints are";
         }
         msg += " inconsistent: ";
         String inconsistentVPs = "";
         for (ViewPoint viewPoint : viewpoints) {
            if (inconsistentVPs.length() > 0) {
               inconsistentVPs += ", ";
            }
            String vpName = viewPoint.getName();
            inconsistentVPs += vpName;
         }
         msg += inconsistentVPs;
      }

      int style = SWT.OK;
      if (0 == size) {
         style = SWT.OK | SWT.ICON_INFORMATION;
      } else {
         style = SWT.OK | SWT.ICON_WARNING;
      }

      Shell shell = WorkbenchUtil.getShell();
      if (shell != null) {
         MessageBox msgBox = new MessageBox(shell, style);
         msgBox.setText("Viewpoint Validation");
         msgBox.setMessage(msg);
         msgBox.open();
      }
   }

   private FeatureMappingModel getFeatureMapping() {
      FeatureMappingModel featureMappingModel = null;
      ClusterMultiPageEditor multiPageEditor = getActiveMultiPageEditor();
      if (multiPageEditor != null) {
         Resource mapping = multiPageEditor.getMappingResource();
         featureMappingModel = FeatureMappingUtil.getFeatureMapping(mapping);
      }
      return featureMappingModel;
   }

   private ClusterMultiPageEditor getActiveMultiPageEditor() {
      ClusterMultiPageEditor mPageEditor = null;
      IEditorPart activeEditor = WorkbenchUtil.getActiveEditor();
      if (activeEditor instanceof ClusterMultiPageEditor) {
         mPageEditor = (ClusterMultiPageEditor) activeEditor;

      }
      return mPageEditor;
   }

   // @Override
   // public boolean isEnabled() {
   // return getFeatureMapping() != null;
   // }

}
