/***********************************************************************
* Copyright by Michael Loesler, https://software.applied-geodesy.org   *
*                                                                      *
* This program is free software; you can redistribute it and/or modify *
* it under the terms of the GNU General Public License as published by *
* the Free Software Foundation; either version 3 of the License, or    *
* at your option any later version.                                    *
*                                                                      *
* This program is distributed in the hope that it will be useful,      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of       *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
* GNU General Public License for more details.                         *
*                                                                      *
* You should have received a copy of the GNU General Public License    *
* along with this program; if not, see <http://www.gnu.org/licenses/>  *
* or write to the                                                      *
* Free Software Foundation, Inc.,                                      *
* 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.            *
*                                                                      *
***********************************************************************/

package org.applied_geodesy.jag3d.ui;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.applied_geodesy.jag3d.DefaultApplicationProperty;
import org.applied_geodesy.jag3d.sql.ProjectDatabaseStateChangeListener;
import org.applied_geodesy.jag3d.sql.ProjectDatabaseStateEvent;
import org.applied_geodesy.jag3d.sql.ProjectDatabaseStateType;
import org.applied_geodesy.jag3d.sql.SQLManager;
import org.applied_geodesy.jag3d.ui.dialog.AboutDialog;
import org.applied_geodesy.jag3d.ui.dialog.AnalysisChartsDialog;
import org.applied_geodesy.jag3d.ui.dialog.ApproximationValuesDialog;
import org.applied_geodesy.jag3d.ui.dialog.AverageDialog;
import org.applied_geodesy.jag3d.ui.dialog.ColumnImportDialog;
import org.applied_geodesy.jag3d.ui.dialog.CongruentPointDialog;
import org.applied_geodesy.jag3d.ui.dialog.FormatterOptionDialog;
import org.applied_geodesy.jag3d.ui.dialog.ImportOptionDialog;
import org.applied_geodesy.jag3d.ui.dialog.LeastSquaresSettingDialog;
import org.applied_geodesy.jag3d.ui.dialog.NetworkAdjustmentDialog;
import org.applied_geodesy.jag3d.ui.dialog.ProjectionAndReductionDialog;
import org.applied_geodesy.jag3d.ui.dialog.RankDefectDialog;
import org.applied_geodesy.jag3d.ui.dialog.SearchAndReplaceDialog;
import org.applied_geodesy.jag3d.ui.dialog.TableRowHighlightDialog;
import org.applied_geodesy.jag3d.ui.dialog.TestStatisticDialog;
import org.applied_geodesy.jag3d.ui.graphic.UIGraphicPaneBuilder;
import org.applied_geodesy.jag3d.ui.graphic.layer.dialog.LayerManagerDialog;
import org.applied_geodesy.jag3d.ui.menu.UIMenuBuilder;
import org.applied_geodesy.jag3d.ui.tabpane.UITabPaneBuilder;
import org.applied_geodesy.jag3d.ui.tree.TreeItemValue;
import org.applied_geodesy.jag3d.ui.tree.UITreeBuilder;
import org.applied_geodesy.ui.dialog.OptionDialog;
import org.applied_geodesy.util.ImageUtils;
import org.applied_geodesy.jag3d.ui.i18n.I18N;
import org.applied_geodesy.util.sql.HSQLDB;
import org.applied_geodesy.version.jag3d.Version;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class JAG3D extends Application {
	
	private class DatabaseStateChangeListener implements ProjectDatabaseStateChangeListener {
		@Override
		public void projectDatabaseStateChanged(ProjectDatabaseStateEvent evt) {
			if (adjustmentButton != null) {
				boolean disable = evt.getEventType() != ProjectDatabaseStateType.OPENED;
				adjustmentButton.setDisable(disable);

				if (evt.getEventType() == ProjectDatabaseStateType.CLOSING || evt.getEventType() == ProjectDatabaseStateType.CLOSED)
					JAG3D.setTitle(null);
			}
		}
	}
	
	private final static String TITLE_TEMPLATE = "%s%sJAG3D%s \u00B7 Least-Squares Adjustment \u0026 Deformation Analysis \u00B7";
	private static Stage primaryStage;
	private Button adjustmentButton;
	
	public static void setTitle(String title) {
		if (primaryStage != null && title != null && !title.trim().isEmpty())
			primaryStage.setTitle(String.format(Locale.ENGLISH, TITLE_TEMPLATE, title, " \u2014 ", (Version.isReleaseCandidate() ? " (RC)" : "")));
		else if (primaryStage != null)
			primaryStage.setTitle(String.format(Locale.ENGLISH, TITLE_TEMPLATE, "", "", (Version.isReleaseCandidate() ? " (RC)" : "")));
	}
	
	public static void close() {
		primaryStage.close();
	}
	
	public static Stage getStage() {
		return primaryStage;
	}
	
	private void setHostServices() {
		HostServices hostServices = this.getHostServices();
		AboutDialog.setHostServices(hostServices);
		SQLManager.setHostServices(hostServices);
		UIMenuBuilder.setHostServices(hostServices);
	}
	
	private void setStageToDialogs(Stage primaryStage) {
		OptionDialog.setOwner(primaryStage);
		NetworkAdjustmentDialog.setOwner(primaryStage);
		FormatterOptionDialog.setOwner(primaryStage);
		TestStatisticDialog.setOwner(primaryStage);
		ProjectionAndReductionDialog.setOwner(primaryStage);
		RankDefectDialog.setOwner(primaryStage);
		CongruentPointDialog.setOwner(primaryStage);
		AverageDialog.setOwner(primaryStage);
		ApproximationValuesDialog.setOwner(primaryStage);
		LeastSquaresSettingDialog.setOwner(primaryStage);
		LayerManagerDialog.setOwner(primaryStage);
		SearchAndReplaceDialog.setOwner(primaryStage);
		AboutDialog.setOwner(primaryStage);
		ColumnImportDialog.setOwner(primaryStage);
		TableRowHighlightDialog.setOwner(primaryStage);
		ImportOptionDialog.setOwner(primaryStage);
		AnalysisChartsDialog.setOwner(primaryStage);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		java.awt.SplashScreen splashScreen = null;
		JAG3D.primaryStage = primaryStage;
		
		try {
			try {
				splashScreen = java.awt.SplashScreen.getSplashScreen();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			I18N i18n = I18N.getInstance();

			UIMenuBuilder menuBuilder = UIMenuBuilder.getInstance();
			UITabPaneBuilder tabPaneBuilder = UITabPaneBuilder.getInstance();
			UITreeBuilder treeBuilder = UITreeBuilder.getInstance();

			TabPane tabPane = tabPaneBuilder.getTabPane();
			TreeView<TreeItemValue> tree = treeBuilder.getTree();

			SplitPane splitPane = new SplitPane();
			splitPane.setOrientation(Orientation.HORIZONTAL);
			splitPane.getItems().addAll(tree, tabPane);
			splitPane.setDividerPositions(0.30);
			SplitPane.setResizableWithParent(tree, false);

			BorderPane border = new BorderPane();
			border.setPrefSize(900, 650);
			border.setTop(menuBuilder.getMenuBar());
			border.setCenter(splitPane);

			UITreeBuilder.getInstance().getTree().getSelectionModel().clearSelection();
			UITreeBuilder.getInstance().getTree().getSelectionModel().selectFirst();

			this.adjustmentButton = new Button(i18n.getString("JavaGraticule3D.button.adjust.label", "Adjust network"));
			this.adjustmentButton.setTooltip(new Tooltip(i18n.getString("JavaGraticule3D.button.adjust.tooltip", "Start network adjustment process")));
			this.adjustmentButton.setOnAction(new EventHandler<ActionEvent>() { 
				@Override
				public void handle(ActionEvent event) {	    	
					NetworkAdjustmentDialog.show();
				}
			});
			this.adjustmentButton.setDisable(true);

			DropShadow ds = new DropShadow();
			ds.setOffsetY(0.5f);
			ds.setColor(Color.gray(0.8));

			Text applicationName = new Text();
			applicationName.setEffect(ds);
			applicationName.setCache(true);
			applicationName.setFill(Color.GREY);
			applicationName.setText("Java\u00B7Applied\u00B7Geodesy\u00B73D");
			applicationName.setFont(Font.font("SansSerif", FontWeight.NORMAL, 17));

			Region spacer = new Region();
			HBox hbox = new HBox(10);
			hbox.setPadding(new Insets(5, 10, 5, 15));
			HBox.setHgrow(spacer, Priority.ALWAYS);
			hbox.getChildren().addAll(applicationName, spacer, this.adjustmentButton);
			border.setBottom(hbox);

			Scene scene = new Scene(border);
			
			scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
				final KeyCombination zoomInKeyComb     = new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN);
				final KeyCombination zoomOutKeyComb    = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN);
				final KeyCombination zoomInKeyCombNum  = new KeyCodeCombination(KeyCode.ADD, KeyCombination.CONTROL_DOWN);
				final KeyCombination zoomOutKeyCombNum = new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.CONTROL_DOWN);
			    public void handle(KeyEvent keyEvent) {
			        if (keyEvent.getCode() == KeyCode.F5 && !adjustmentButton.isDisabled()) {
			        	adjustmentButton.fire();
			        	keyEvent.consume();
			        }
			        else if ((zoomInKeyComb.match(keyEvent) || zoomInKeyCombNum.match(keyEvent)) && tabPane.getSelectionModel().getSelectedItem() != null && tabPane.getSelectionModel().getSelectedItem().getContent() == UIGraphicPaneBuilder.getInstance().getPane()) {
			        	UIGraphicPaneBuilder.getInstance().getLayerManager().zoomIn();
			        	keyEvent.consume();
			        }
			        else if ((zoomOutKeyComb.match(keyEvent) || zoomOutKeyCombNum.match(keyEvent)) && tabPane.getSelectionModel().getSelectedItem() != null && tabPane.getSelectionModel().getSelectedItem().getContent() == UIGraphicPaneBuilder.getInstance().getPane()) {
			        	UIGraphicPaneBuilder.getInstance().getLayerManager().zoomOut();
			        	keyEvent.consume();
			        }
			    }
			});

			try {
				primaryStage.getIcons().addAll(
						ImageUtils.getImage("JAG3D_16x16.png"),
						ImageUtils.getImage("JAG3D_32x32.png"),
						ImageUtils.getImage("JAG3D_64x64.png")
						);
			} 
			catch(Exception e) {
				e.printStackTrace();
			}
			primaryStage.setScene(scene);

			setTitle(null);

			primaryStage.show();
			primaryStage.setMaximized(DefaultApplicationProperty.startApplicationInFullScreen());
			primaryStage.toFront();

			this.setStageToDialogs(primaryStage);
			this.setHostServices();
			SQLManager.getInstance().addProjectDatabaseStateChangeListener(new DatabaseStateChangeListener());
			
			try {
				// check for command line arguments
				// --database=D:\\data\\project.script
				// --ut
				final Parameters params = this.getParameters();
				final Map<String, String> parameterMap = params.getNamed();
				if (parameterMap.containsKey("database")) {
					Path path = Paths.get (parameterMap.get("database") );
					String regex = "(?i)(.+?)(\\.)(backup$|data$|properties$|script$)";
					String project = Files.exists(path, LinkOption.NOFOLLOW_LINKS) ? path.toAbsolutePath().toString().replaceFirst(regex, "$1") : null;

					if (project != null) {
						SQLManager.openExistingProject(new HSQLDB(project));
						JAG3D.setTitle(path.getFileName() == null ? null : path.getFileName().toString().replaceFirst(regex, "$1"));
					}
				}
				if (parameterMap.containsKey("ut") && parameterMap.getOrDefault("ut", "FALSE").equalsIgnoreCase("TRUE")) {
					LeastSquaresSettingDialog.setEnableUnscentedTransformation(true);
					org.applied_geodesy.juniform.ui.dialog.LeastSquaresSettingDialog.setEnableUnscentedTransformation(true);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		finally {
			if (splashScreen != null)
				splashScreen.close();
		}
	}
	
	public void stop() throws Exception {
		SQLManager.getInstance().closeDataBase();
		super.stop();
	}

	public static void main(String[] args) {
		//Locale.setDefault(Locale.ENGLISH);
		//Locale.setDefault(new Locale("sr", "BA"));
		try {
			Logger[] loggers = new Logger[] {
					Logger.getLogger("hsqldb.db"),
					Logger.getLogger("com.github.fommil.netlib.LAPACK"),
					Logger.getLogger("com.github.fommil.netlib.BLAS")
			};

			for (Logger logger : loggers) {
				logger.setUseParentHandlers(false);
				logger.setLevel(Level.OFF);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		Application.launch(JAG3D.class, args);
    }
}
