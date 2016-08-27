package com.igormaznitsa.nbgolang.projtemplate;

import java.awt.Component;
import java.util.HashSet;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

public class MvnGolangProjectWizardPanel implements WizardDescriptor.Panel, WizardDescriptor.ValidatingPanel, WizardDescriptor.FinishablePanel {

  private WizardDescriptor wizardDescriptor;
  private MvnGolangProjectPanelVisual component;

  public MvnGolangProjectWizardPanel() {
  }

  @Override
  public Component getComponent() {
    if (component == null) {
      component = new MvnGolangProjectPanelVisual(this);
      component.setName(NbBundle.getMessage(MvnGolangProjectWizardPanel.class, "LBL_CreateProjectStep"));
    }
    return component;
  }

  @Override
  public HelpCtx getHelp() {
    return new HelpCtx(MvnGolangProjectWizardPanel.class);
  }

  @Override
  public boolean isValid() {
    getComponent();
    return component.valid(wizardDescriptor);
  }

  private final Set<ChangeListener> listeners = new HashSet<ChangeListener>();

  @Override
  public final void addChangeListener(final ChangeListener l) {
    synchronized (listeners) {
      listeners.add(l);
    }
  }

  @Override
  public final void removeChangeListener(final ChangeListener l) {
    synchronized (listeners) {
      listeners.remove(l);
    }
  }

  protected final void fireChangeEvent() {
    Set<ChangeListener> ls;
    synchronized (this.listeners) {
      ls = new HashSet<ChangeListener>(this.listeners);
    }
    final ChangeEvent ev = new ChangeEvent(this);
    for (final ChangeListener l : ls) {
      l.stateChanged(ev);
    }
  }

  @Override
  public void readSettings(final Object settings) {
    this.wizardDescriptor = (WizardDescriptor) settings;
    this.component.read(this.wizardDescriptor);
  }

  @Override
  public void storeSettings(final Object settings) {
    final WizardDescriptor d = (WizardDescriptor) settings;
    this.component.store(d);
  }

  @Override
  public boolean isFinishPanel() {
    return true;
  }

  @Override
  public void validate() throws WizardValidationException {
    getComponent();
    this.component.validate(wizardDescriptor);
  }

}
