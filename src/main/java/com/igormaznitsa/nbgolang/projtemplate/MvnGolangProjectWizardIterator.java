package com.igormaznitsa.nbgolang.projtemplate;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

@TemplateRegistration(folder = "Project/Maven2", position = -1000, displayName = "#MvnGolangProject_displayName", description = "MvnGolangProjectDescription.html", iconBase = "com/igormaznitsa/nbgolang/projtemplate/MvnGolangProject.png", content = "mvngolangproj.zip")
@Messages("MvnGolangProject_displayName=Golang Application")
public class MvnGolangProjectWizardIterator implements WizardDescriptor./*Progress*/InstantiatingIterator {

  public static final String WRAPPER_VERSION = "2.1.1";
  public static final String SDK_VERSION = "1.7";

  private int index;
  private WizardDescriptor.Panel[] panels;
  private WizardDescriptor wiz;

  public MvnGolangProjectWizardIterator() {
  }

  public static MvnGolangProjectWizardIterator createIterator() {
    return new MvnGolangProjectWizardIterator();
  }

  private WizardDescriptor.Panel[] createPanels() {
    return new WizardDescriptor.Panel[]{
      new MvnGolangProjectWizardPanel(),};
  }

  private String[] createSteps() {
    return new String[]{
      NbBundle.getMessage(MvnGolangProjectWizardIterator.class, "LBL_CreateProjectStep")
    };
  }

  @Override
  public Set/*<FileObject>*/ instantiate(/*ProgressHandle handle*/) throws IOException {
    final Set<FileObject> resultSet = new LinkedHashSet<FileObject>();
    final File dirF = FileUtil.normalizeFile((File) wiz.getProperty(MvnGolangProjectPanelVisual.STORE_PROJECT_FOLDER));
    dirF.mkdirs();

    final Properties props = new Properties();
    props.put(MvnGolangProjectPanelVisual.STORE_ARTIFACT_ID, wiz.getProperty(MvnGolangProjectPanelVisual.STORE_ARTIFACT_ID));
    props.put(MvnGolangProjectPanelVisual.STORE_GROUP_ID, wiz.getProperty(MvnGolangProjectPanelVisual.STORE_GROUP_ID));
    props.put(MvnGolangProjectPanelVisual.STORE_VERSION, wiz.getProperty(MvnGolangProjectPanelVisual.STORE_VERSION));
    props.put(MvnGolangProjectPanelVisual.STORE_NAME, wiz.getProperty(MvnGolangProjectPanelVisual.STORE_NAME));

    final FileObject template = Templates.getTemplate(wiz);
    final FileObject dir = FileUtil.toFileObject(dirF);
    unZipFile(template.getInputStream(), props, dir);

    // Always open top dir as a project:
    resultSet.add(dir);
    // Look for nested projects to open as well:
    final Enumeration<? extends FileObject> e = dir.getFolders(true);
    while (e.hasMoreElements()) {
      FileObject subfolder = e.nextElement();
      if (ProjectManager.getDefault().isProject(subfolder)) {
        resultSet.add(subfolder);
      }
    }

    final File parent = dirF.getParentFile();
    if (parent != null && parent.exists()) {
      ProjectChooser.setProjectsFolder(parent);
    }

    return resultSet;
  }

  @Override
  public void initialize(final WizardDescriptor wiz) {
    this.wiz = wiz;
    index = 0;
    panels = createPanels();
    // Make sure list of steps is accurate.
    final String[] steps = createSteps();
    for (int i = 0; i < panels.length; i++) {
      Component c = panels[i].getComponent();
      if (steps[i] == null) {
        // Default step name to component name of panel.
        // Mainly useful for getting the name of the target
        // chooser to appear in the list of steps.
        steps[i] = c.getName();
      }
      if (c instanceof JComponent) { // assume Swing components
        final JComponent jc = (JComponent) c;
        // Step #.
        // TODO if using org.openide.dialogs >= 7.8, can use WizardDescriptor.PROP_*:
        jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i));
        // Step name (actually the whole list for reference).
        jc.putClientProperty("WizardPanel_contentData", steps);
      }
    }
  }

  @Override
  public void uninitialize(final WizardDescriptor wiz) {
    this.wiz.putProperty(MvnGolangProjectPanelVisual.STORE_PROJECT_FOLDER, null);
    this.wiz.putProperty(MvnGolangProjectPanelVisual.STORE_NAME, null);
    this.wiz.putProperty(MvnGolangProjectPanelVisual.STORE_ARTIFACT_ID, null);
    this.wiz.putProperty(MvnGolangProjectPanelVisual.STORE_GROUP_ID, null);
    this.wiz.putProperty(MvnGolangProjectPanelVisual.STORE_VERSION, null);
    this.wiz = null;
    panels = null;
  }

  @Override
  public String name() {
    return MessageFormat.format("{0} of {1}", new Object[]{new Integer(index + 1), new Integer(panels.length)});
  }

  @Override
  public boolean hasNext() {
    return index < panels.length - 1;
  }

  @Override
  public boolean hasPrevious() {
    return index > 0;
  }

  @Override
  public void nextPanel() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    index++;
  }

  @Override
  public void previousPanel() {
    if (!hasPrevious()) {
      throw new NoSuchElementException();
    }
    index--;
  }

  @Override
  public WizardDescriptor.Panel current() {
    return panels[index];
  }

  @Override
  public final void addChangeListener(final ChangeListener l) {
  }

  @Override
  public final void removeChangeListener(final ChangeListener l) {
  }

  private static void unZipFile(final InputStream source, final Properties props, final FileObject projectRoot) throws IOException {
    try {
      final ZipInputStream str = new ZipInputStream(source);
      ZipEntry entry;
      while ((entry = str.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          FileUtil.createFolder(projectRoot, entry.getName());
        } else {
          FileObject fo = FileUtil.createData(projectRoot, entry.getName());
          if ("pom.xml".equals(entry.getName())) {
            // Special handling for setting name of Ant-based projects; customize as needed:
            filterPomXml(fo, str, projectRoot.getName(), props);
          } else {
            writeFile(str, fo);
          }
        }
      }
    } finally {
      source.close();
    }
  }

  private static void writeFile(final InputStream str, final FileObject fo) throws IOException {
    final OutputStream out = fo.getOutputStream();
    try {
      FileUtil.copy(str, out);
    } finally {
      out.close();
    }
  }

  private static void filterPomXml(final FileObject fo, final ZipInputStream str, final String name, final Properties props) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    FileUtil.copy(str, baos);

    String text = new String(baos.toByteArray(), "UTF-8");

    text = text.replace("${{group.id}}", props.getProperty(MvnGolangProjectPanelVisual.STORE_GROUP_ID))
        .replace("${{artifact.id}}", props.getProperty(MvnGolangProjectPanelVisual.STORE_ARTIFACT_ID))
        .replace("${{version}}", props.getProperty(MvnGolangProjectPanelVisual.STORE_VERSION))
        .replace("${{plugin.version}}", WRAPPER_VERSION)
        .replace("${{sdk.version}}", SDK_VERSION);

    writeFile(new ByteArrayInputStream(text.getBytes("UTF-8")), fo);
  }

}
